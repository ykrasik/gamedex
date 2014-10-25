package com.github.ykrasik.indexter.games.spring;

import com.github.ykrasik.indexter.games.data.GameDataListener;
import com.github.ykrasik.indexter.games.data.GameDataService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class GameDataListenerBeanProcessor implements BeanPostProcessor {
    private final GameDataService dataService;

    public GameDataListenerBeanProcessor(GameDataService dataService) {
        this.dataService = Objects.requireNonNull(dataService);
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof GameDataListener) {
            dataService.addListener((GameDataListener) bean);
        }
        return bean;
    }
}
