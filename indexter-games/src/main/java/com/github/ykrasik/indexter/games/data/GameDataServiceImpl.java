package com.github.ykrasik.indexter.games.data;

import com.github.ykrasik.indexter.games.info.GameDetailedInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yevgeny Krasik
 */
public class GameDataServiceImpl implements GameDataService {
    private final Map<String, GameDetailedInfo> data;

    public GameDataServiceImpl() {
        this.data = new HashMap<>();
    }

    @Override
    public void add(GameDetailedInfo info) throws IllegalArgumentException {
        if (data.containsKey(info.getName())) {
            throw new IllegalArgumentException("Already have an entry for: " + info.getName());
        }
        data.put(info.getName(), info);
    }

    @Override
    public Collection<GameDetailedInfo> getAll() {
        return data.values();
    }
}
