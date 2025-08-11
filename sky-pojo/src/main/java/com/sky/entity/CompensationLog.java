package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * -- sky_take_out.compensation_log definition
 *
 * CREATE TABLE `compensation_log` (
 *   `id` int NOT NULL AUTO_INCREMENT,
 *   `orderId` bigint NOT NULL,
 *   `beforeStatus` varchar(100) DEFAULT NULL,
 *   `afterStatus` varchar(100) DEFAULT NULL,
 *   `reason` varchar(100) DEFAULT NULL,
 *   `operator` varchar(100) DEFAULT NULL,
 *   `operateTime` datetime NULL DEFAULT NULL,
 *   PRIMARY KEY (`id`)
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CompensationLog {

    private Long orderId;
    private String beforeStatus;
    private String afterStatus;
    private String reason;
    private String operator;
    private LocalDateTime operateTime;

}
