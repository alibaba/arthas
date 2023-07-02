package com.alibaba.arthas.tunnel.server.app.feature.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * Arthas Agent 分组
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.7
 */
@EqualsAndHashCode
@ToString
@Data
public class ArthasAgentGroup {

    private String service;

    private List<ArthasAgent> agents;
}
