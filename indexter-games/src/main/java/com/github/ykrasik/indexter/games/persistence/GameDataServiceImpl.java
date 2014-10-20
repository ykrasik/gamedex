package com.github.ykrasik.indexter.games.persistence;

import com.github.ykrasik.indexter.games.datamodel.GameDetailedInfo;

import java.util.*;

/**
 * @author Yevgeny Krasik
 */
public class GameDataServiceImpl implements GameDataService {
    private final Map<String, GameDetailedInfo> data;
    private final List<GameDataListener> listeners;

    public GameDataServiceImpl() {
        this.data = new HashMap<>();
        this.listeners = new ArrayList<>();
    }

    @Override
    public void add(GameDetailedInfo info) throws IllegalArgumentException {
        doAdd(info);
        notifyListeners();
    }

    @Override
    public void addAll(Collection<GameDetailedInfo> infos) throws IllegalArgumentException {
        infos.forEach(this::doAdd);
        notifyListeners();
    }

    private void doAdd(GameDetailedInfo info) {
        if (data.containsKey(info.getName())) {
            throw new IllegalArgumentException("Already have an entry for: " + info.getName());
        }
        data.put(info.getName(), info);
    }

    @Override
    public Collection<GameDetailedInfo> getAll() {
        return data.values();
    }

    @Override
    public void addListener(GameDataListener listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (GameDataListener listener : listeners) {
            listener.onUpdate(this);
        }
    }
}
