package com.github.ykrasik.gamedex.common.spring;

import com.github.ykrasik.gamedex.common.preloader.Preloader;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Yevgeny Krasik
 */
public class AbstractBeanConfiguration {
    @Autowired
    protected Preloader preloader;
}
