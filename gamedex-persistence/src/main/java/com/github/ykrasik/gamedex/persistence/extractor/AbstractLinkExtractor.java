package com.github.ykrasik.gamedex.persistence.extractor;

import com.github.ykrasik.gamedex.common.util.ListUtils;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.opt.Opt;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public abstract class AbstractLinkExtractor<DataModel, LinkEntity> {
    @NonNull private final Map<Id<DataModel>, DataModel> dataModelMap;
    @NonNull private final Map<Integer, List<LinkEntity>> linkEntityMap;

    public List<DataModel> getData(int dataModelId) {
        final Opt<List<LinkEntity>> linkEntities = Opt.ofNullable(linkEntityMap.get(dataModelId));
        return linkEntities.map(this::linkEntitiesToGenre).getOrElse(Collections.emptyList());
    }

    private List<DataModel> linkEntitiesToGenre(List<LinkEntity> linkEntities) {
        final List<Id<DataModel>> dataModelIds = ListUtils.map(linkEntities, this::getLinkDataModelId);
        return ListUtils.map(dataModelIds, dataModelMap::get);
    }

    protected abstract Id<DataModel> getLinkDataModelId(LinkEntity linkEntity);
}
