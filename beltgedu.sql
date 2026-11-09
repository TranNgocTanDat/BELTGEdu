-- MySQL dump 10.13  Distrib 8.0.34, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: beltgedu
-- ------------------------------------------------------
-- Server version	8.0.35

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `regions`
--

DROP TABLE IF EXISTS `regions`;
CREATE TABLE `regions` (
  `id` binary(16) NOT NULL,
  `name` varchar(120) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_regions_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table structure for table `schools`
--

DROP TABLE IF EXISTS `schools`;
CREATE TABLE `schools` (
  `id` binary(16) NOT NULL,
  `name` varchar(200) NOT NULL,
  `code` varchar(40) NOT NULL,
  `address` varchar(500) DEFAULT NULL,
  `contact_name` varchar(150) DEFAULT NULL,
  `phone` varchar(40) DEFAULT NULL,
  `zalo_group_stem` varchar(300) DEFAULT NULL,
  `zalo_group_digital` varchar(300) DEFAULT NULL,
  `level` varchar(20) NOT NULL,
  `created_at` date NOT NULL,
  `region_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schools_code` (`code`),
  KEY `idx_schools_region` (`region_id`),
  CONSTRAINT `fk_schools_region` FOREIGN KEY (`region_id`) REFERENCES `regions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table structure for table `announcements`
--

DROP TABLE IF EXISTS `announcements`;
CREATE TABLE `announcements` (
  `id` binary(16) NOT NULL,
  `announcement_key` varchar(220) NOT NULL,
  `registration_id` varchar(100) NOT NULL,
  `service` varchar(16) NOT NULL,
  `week_start` date NOT NULL,
  `message` text NOT NULL,
  `announced_at` datetime(6) NOT NULL,
  `school_id` binary(16) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_announcement_key` (`announcement_key`),
  KEY `idx_announcements_school_week` (`school_id`,`week_start`),
  CONSTRAINT `fk_announcements_school` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table structure for table `school_closures`
--

DROP TABLE IF EXISTS `school_closures`;
CREATE TABLE `school_closures` (
  `id` binary(16) NOT NULL,
  `school_id` binary(16) NOT NULL,
  `week_start` date NOT NULL,
  `reason` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_school_closure_week` (`school_id`,`week_start`),
  CONSTRAINT `fk_school_closures_school` FOREIGN KEY (`school_id`) REFERENCES `schools` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table structure for table `stem_courses`
--

DROP TABLE IF EXISTS `stem_lessons`;
DROP TABLE IF EXISTS `stem_courses`;
CREATE TABLE `stem_courses` (
  `id` varchar(80) NOT NULL,
  `code` varchar(40) NOT NULL,
  `program` varchar(16) NOT NULL,
  `subject` varchar(24) NOT NULL,
  `grade` int DEFAULT NULL,
  `module` int DEFAULT NULL,
  `part` int NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table structure for table `stem_lessons`
--

CREATE TABLE `stem_lessons` (
  `id` varchar(100) NOT NULL,
  `course_id` varchar(80) NOT NULL,
  `lesson_order` int NOT NULL,
  `title` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stem_lesson_order` (`course_id`,`lesson_order`),
  CONSTRAINT `fk_stem_lessons_course` FOREIGN KEY (`course_id`) REFERENCES `stem_courses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table structure for table `digital_lessons`
--

CREATE TABLE `digital_lessons` (
  `id` varchar(100) NOT NULL,
  `program` varchar(16) NOT NULL,
  `grade` int NOT NULL,
  `lesson_order` int NOT NULL,
  `title` varchar(500) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_digital_lesson_position` (`program`,`grade`,`lesson_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` binary(16) NOT NULL,
  `dob` date DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `username` varchar(255) DEFAULT NULL,
  `verification_code` varchar(255) DEFAULT NULL,
  `verification_expiration` datetime(6) DEFAULT NULL,
  `region_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_region` (`region_id`),
  CONSTRAINT `fk_user_region` FOREIGN KEY (`region_id`) REFERENCES `regions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (_binary 'Od/ó*@\Âè3π\Ó»æy¶',NULL,'admin@ltg.edu.vn',_binary '',NULL,NULL,'$2a$10$61eiFuj98ezOlFIpIQxPruAljFYiu8FX9U6faumRHe.BGcpmExxYS','LTG Education',NULL,NULL,NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_roles`
--

DROP TABLE IF EXISTS `user_roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_roles` (
  `user_id` binary(16) NOT NULL,
  `roles` varchar(255) DEFAULT NULL,
  KEY `FK55itppkw3i07do3h7qoclqd4k` (`user_id`),
  CONSTRAINT `FK55itppkw3i07do3h7qoclqd4k` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_roles`
--

LOCK TABLES `user_roles` WRITE;
/*!40000 ALTER TABLE `user_roles` DISABLE KEYS */;
INSERT INTO `user_roles` VALUES (_binary 'Od/ó*@\Âè3π\Ó»æy¶','ADMIN');
/*!40000 ALTER TABLE `user_roles` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-01 16:25:53
