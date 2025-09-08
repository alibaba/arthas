package org.example.jfranalyzerbackend.service;

import org.example.jfranalyzerbackend.vo.FlameGraph;
import org.example.jfranalyzerbackend.vo.Metadata;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JFR分析服务测试类
 */
import java.util.*;

public class JFRAnalysisServiceTest {
    public boolean canFinish(int numCourses, int[][] prerequisites) {
        // 入度表
        int[] indegree = new int[numCourses];
        // 邻接表
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < numCourses; i++) {
            graph.add(new ArrayList<>());
        }

        // 构建图
        for (int[] pre : prerequisites) {
            int course = pre[0];
            int preCourse = pre[1];
            graph.get(preCourse).add(course);
            indegree[course]++;
        }

        // 队列存放入度为0的课程
        Queue<Integer> queue = new LinkedList<>();
        for (int i = 0; i < numCourses; i++) {
            if (indegree[i] == 0) {
                queue.offer(i);
            }
        }

        int count = 0; // 已完成的课程数量
        while (!queue.isEmpty()) {
            int course = queue.poll();
            count++;
            for (int next : graph.get(course)) {
                indegree[next]--;
                if (indegree[next] == 0) {
                    queue.offer(next);
                }
            }
        }

        return count == numCourses;
    }

    // 测试方法
    public static void main(String[] args) {
         JFRAnalysisServiceTest solver = new JFRAnalysisServiceTest();
//
//        // 示例 1：简单的线性依赖
//        int numCourses1 = 2;
//        int[][] prerequisites1 = {{1, 0}};
//        System.out.println("Example 1: " + solver.canFinish(numCourses1, prerequisites1));
//        // true，因为可以先学0再学1
//
//        // 示例 2：有环的情况
//        int numCourses2 = 2;
//        int[][] prerequisites2 = {{1, 0}, {0, 1}};
//        System.out.println("Example 2: " + solver.canFinish(numCourses2, prerequisites2));
//        // false，因为 0 -> 1 -> 0 出现环
//
//        // 示例 3：多个前置课程
//        int numCourses3 = 4;
//        int[][] prerequisites3 = {{1, 0}, {2, 0}, {3, 1}, {3, 2}};
//        System.out.println("Example 3: " + solver.canFinish(numCourses3, prerequisites3));
//        // true，可以学习顺序：0 -> 1 -> 2 -> 3

        // 示例 4：复杂依赖但无环
        int numCourses4 = 5;
        int[][] prerequisites4 = {{1,0},{2,0},{3,1},{3,2},{4,3}};
        System.out.println("Example 4: " + solver.canFinish(numCourses4, prerequisites4));
        // true，顺序：0 -> 1 -> 2 -> 3 -> 4

        // 示例 5：复杂依赖有环
        int numCourses5 = 3;
        int[][] prerequisites5 = {{0,1},{1,2},{2,0}};
        System.out.println("Example 5: " + solver.canFinish(numCourses5, prerequisites5));
        // false，因为 0 -> 1 -> 2 -> 0 出现环
    }
}

