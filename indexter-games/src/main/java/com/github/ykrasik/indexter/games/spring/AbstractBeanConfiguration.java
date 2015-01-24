package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.IndexterPreloader;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yevgeny Krasik
 */
public class AbstractBeanConfiguration {
    @Autowired
    protected IndexterPreloader preloader;
}
