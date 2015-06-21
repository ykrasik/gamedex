package com.github.ykrasik.gamedex.persistence.extractor;

import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.yava.option.Opt;
import com.gs.collections.api.collection.ImmutableCollection;
import com.gs.collections.api.map.ImmutableMap;
import com.gs.collections.api.multimap.ImmutableMultimap;
import com.gs.collections.impl.factory.Lists;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public abstract class AbstractLinkExtractor<DataModel, LinkEntity> {
    @NonNull private final ImmutableMap<Id<DataModel>, DataModel> dataModelMap;
    @NonNull private final ImmutableMultimap<Integer, LinkEntity> linkEntityMap;

    public ImmutableCollection<DataModel> getData(int dataModelId) {
        final Opt<ImmutableCollection<LinkEntity>> linkEntities = Opt.ofNullable(linkEntityMap.get(dataModelId));
        return linkEntities.map(this::linkEntitiesToGenre).getOrElse(Lists.immutable.of());
    }

    private ImmutableCollection<DataModel> linkEntitiesToGenre(ImmutableCollection<LinkEntity> linkEntities) {
        final ImmutableCollection<Id<DataModel>> dataModelIds = linkEntities.collect(this::getLinkDataModelId);
        return dataModelIds.collect(dataModelMap::get);
    }

    protected abstract Id<DataModel> getLinkDataModelId(LinkEntity linkEntity);
}
