/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.execution.plan;

import org.gradle.api.NonNullApi;
import org.gradle.internal.taskgraph.CalculateTaskGraphBuildOperationType;
import org.gradle.internal.taskgraph.CalculateTaskGraphBuildOperationType.TaskIdentity;
import org.gradle.internal.taskgraph.NodeIdentity;

import java.util.List;

/**
 * Converts a node to a planned node.
 * <p>
 * Each implementation of this interface is responsible for nodes of {@link #getSupportedNodeType()}.
 * The converter can obtain the node identity for each node of the supported type via {@link #getNodeIdentity(Node)}.
 */
@NonNullApi
public interface ToPlannedNodeConverter {

    /**
     * Type of node that this converter can identify and convert to a planned node.
     */
    Class<? extends Node> getSupportedNodeType();

    /**
     * Provides a unique identity for the node of the {@link #getSupportedNodeType() supported type}.
     */
    NodeIdentity getNodeIdentity(Node node);

    /**
     * Returns true if the given {@link Node} is from the same execution plan.
     * <p>
     * A node can be in another execution plan if it is, for instance, from an included build ({@link TaskInAnotherBuild}).
     */
    boolean isInSamePlan(Node node);

    /**
     * Converts a node to a planned node.
     * <p>
     * Expects a node of the {@link #getSupportedNodeType() supported type} that is in the {@link #isInSamePlan(Node) same plan}.
     */
    CalculateTaskGraphBuildOperationType.PlannedNode convert(Node node, DependencyLookup dependencyLookup);

    interface DependencyLookup {

        /**
         * Finds all identifiable dependencies of the given node.
         */
        List<? extends NodeIdentity> findNodeDependencies(Node node);

        /**
         * Finds dependencies that are task nodes, skipping over other nodes.
         * <p>
         * This is required for compatibility reasons for the {@link ToPlannedTaskConverter}.
         */
        List<? extends TaskIdentity> findTaskDependencies(Node node);
    }
}
