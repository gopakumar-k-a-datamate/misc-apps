package org.datamate.identity.shared.config.security;

import com.datamate.bedrock.framework.common.auditing.envers.vo.RevisionMetadataHolder;
import com.datamate.bedrock.framework.common.auditing.service.MetadataProvider;
import com.datamate.bedrock.framework.common.logging.annotation.EnableLogger;
import com.datamate.bedrock.framework.common.logging.service.Logger;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RevisionMetadataFilter implements Filter {

    @EnableLogger
    private Logger log;

    private final MetadataProvider metadataProvider;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        log.debug("Setting revision metadata for request");
        try {
            RevisionMetadataHolder.set(metadataProvider);
            chain.doFilter(request, response);
        } finally {
            RevisionMetadataHolder.clear();
        }
    }
}
