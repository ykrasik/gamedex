package com.github.ykrasik.gamedex.core.spring;

import com.github.ykrasik.gamedex.core.preloader.Preloader;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yevgeny Krasik
 */
public class AbstractBeanConfiguration {
    @Autowired
    protected Preloader preloader;
}
