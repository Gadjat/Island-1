package ru.javarush.island.zazimko.entity.organizms;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import ru.javarush.island.zazimko.abstraction.entity.Reproducible;
import ru.javarush.island.zazimko.config.Setting;
import ru.javarush.island.zazimko.entity.map.Cell;
import ru.javarush.island.zazimko.entity.map.Residents;
import ru.javarush.island.zazimko.util.Randoms;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("unused")
@Getter
@EqualsAndHashCode(of = "id")
public abstract class Organism implements Reproducible, Cloneable {

    private final static AtomicLong idCounter = new AtomicLong(System.currentTimeMillis());

    public Organism(String name, String icon, Limit limit) {
        this.name = name;
        this.icon = icon;
        this.limit = limit;
        weight = Randoms.random(limit.getMaxWeight() / 2, limit.getMaxWeight());
    }

    private long id = idCounter.incrementAndGet();
    private final String type = this
            .getClass()
            .getSimpleName();
    private final String name;
    private final String icon;

    private transient final String letter = type.substring(0, 1);
    @Setter
    private double weight;
    private final Limit limit;

    @Override
    public String toString() {
        return icon;
    }

    //TODO  Coding. Ctrl_C Ctrl_V ??? But we like original coding.
    @Override
    protected Organism clone() throws CloneNotSupportedException {
        //visible in inherits (cast to Organism)
        Organism clone = (Organism) super.clone();
        clone.id = idCounter.incrementAndGet();
        clone.weight = Randoms.random(limit.getMaxWeight() / 2, limit.getMaxWeight());
        return clone;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Organism> T clone(T original) {
        //for clients (cast to original Type)
        try {
            return (T) original.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }

    }


    protected boolean safeDie(Cell cell) {
        cell.getLock().lock();
        try {
            return cell
                    .getResidents()
                    .get(type)
                    .remove(this);
        } finally {
            cell.getLock().unlock();
        }
    }

    protected boolean safeChangeWeight(Cell currentCell, int percent) {
        currentCell.getLock().lock();
        try {
            double maxWeight = limit.getMaxWeight();
            weight += maxWeight * percent / 100;
            weight = Math.max(0, weight);
            weight = Math.min(weight, maxWeight);
            return currentCell
                    .getResidents()
                    .get(type)
                    .contains(this);
        } finally {
            currentCell.getLock().unlock();
        }
    }


    protected boolean safeMove(Cell source, Cell destination) {
        if (safeAddTo(destination)) {
            if (safePollFrom(source)) {
                return true;
            } else {
                safePollFrom(destination);
            }
        }
        return false;
    }

    protected boolean safeAddTo(Cell cell) {
        cell.getLock().lock();
        try {
            Organisms organisms = cell
                    .getResidents()
                    .get(getType());
            int maxCount = getLimit().getMaxCountInCell();
            int size = organisms.size();
            return size < maxCount && organisms.add(this);
        } finally {
            cell.getLock().unlock();
        }
    }

    protected boolean safePollFrom(Cell cell) {
        cell.getLock().lock();
        try {
            Residents residents = cell.getResidents();
            Organisms organisms = residents.get(getType());
            return organisms.remove(this);
        } finally {
            cell.getLock().unlock();
        }
    }

    protected boolean safeFindFood(Cell currentCell) {
        currentCell.getLock().lock();
        boolean foodFound = false;
        try {
            double needFood = getNeedFood();
            if (!(needFood <= 0)) {
                Setting setting = Setting.get();
                var foodMap = setting
                        .getFoodMap(getType())
                        .entrySet();
                var foodIterator = foodMap.iterator();
                while (needFood > 0 && foodIterator.hasNext()) {
                    Map.Entry<String, Integer> entry = foodIterator.next();
                    String keyFood = entry.getKey();
                    Integer probably = entry.getValue();
                    Residents residents = currentCell.getResidents();
                    residents.randomRotateResidents();
                    var foods = residents.get(keyFood);
                    if (Objects.nonNull(foods) && !foods.isEmpty() && Randoms.get(probably)) {
                        for (Iterator<Organism> organismIterator = foods.iterator(); organismIterator.hasNext(); ) {
                            Organism o = organismIterator.next();
                            double foodWeight = o.getWeight();
                            double delta = Math.min(foodWeight, needFood);
                            setWeight(getWeight() + delta);
                            o.setWeight(foodWeight - delta);
                            if (o.getWeight() <= 0) {
                                organismIterator.remove();
                            }
                            needFood -= delta;
                            foodFound = true;
                            if (needFood <= 0) {
                                break;
                            }
                        }
                    }
                }
            }
        } finally {
            currentCell.getLock().unlock();
        }
        return foodFound;
    }

    private double getNeedFood() {
        return Math.min(
                getLimit().getMaxFood(),
                getLimit().getMaxWeight() - getWeight());
    }

}
