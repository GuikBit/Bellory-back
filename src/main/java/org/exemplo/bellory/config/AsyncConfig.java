package org.exemplo.bellory.config;

import org.exemplo.bellory.context.TenantContext;
import org.exemplo.bellory.model.entity.config.ConfigSistema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("bellory-async-");
        executor.setTaskDecorator(new TenantContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncExceptionHandler();
    }

    private static class TenantContextTaskDecorator implements TaskDecorator {

        @Override
        public Runnable decorate(Runnable runnable) {
            Long organizacaoId = TenantContext.getCurrentOrganizacaoId();
            Long userId = TenantContext.getCurrentUserId();
            String username = TenantContext.getCurrentUsername();
            String role = TenantContext.getCurrentRole();
            ConfigSistema configSistema = TenantContext.getCurrentConfigSistema();

            return () -> {
                try {
                    TenantContext.setContext(organizacaoId, userId, username, role);
                    TenantContext.setCurrentConfigSistema(configSistema);
                    runnable.run();
                } finally {
                    TenantContext.clear();
                }
            };
        }
    }

    private static class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("Excecao async nao tratada no metodo {}.{}",
                    method.getDeclaringClass().getSimpleName(), method.getName(), ex);
        }
    }
}
