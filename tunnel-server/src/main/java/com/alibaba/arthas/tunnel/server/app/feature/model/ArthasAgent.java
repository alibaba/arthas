package com.alibaba.arthas.tunnel.server.app.feature.model;

import com.alibaba.arthas.tunnel.server.AgentInfo;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Arthas Agent
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.7
 */
@EqualsAndHashCode
@ToString
@Data
public class ArthasAgent {

    private String id;

    private AgentInfo info;
}
