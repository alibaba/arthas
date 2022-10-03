package com.alibaba.arthas.tunnel.proxy.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

/**
 * Arthas Agent 分组
 *
 * @author <a href="mailto:shiyindaxiaojie@gmail.com">gyl</a>
 * @since 3.6.6
 */
@EqualsAndHashCode
@ToString
@Data
public class ArthasAgentGroup {

    private String serviceId;

    private List<ArthasAgent> arthasAgents;
}
