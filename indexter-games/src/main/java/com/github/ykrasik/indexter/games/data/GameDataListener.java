package com.github.ykrasik.indexter.games.data;

import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;

import java.util.Collection;

/**
 * @author Yevgeny Krasik
 */
public interface GameDataListener {
    void onUpdate(Collection<LocalGameInfo> newOrUpdatedInfos);
}
