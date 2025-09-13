-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: apartment_db
-- ------------------------------------------------------
-- Server version	8.4.5

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
-- Table structure for table `invoice`
--

DROP TABLE IF EXISTS `invoice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `invoice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `billing_month` int NOT NULL,
  `billing_year` int NOT NULL,
  `due_date` date NOT NULL,
  `electricity_baht` decimal(12,2) DEFAULT NULL,
  `electricity_rate` decimal(12,2) DEFAULT NULL,
  `electricity_units` decimal(12,2) DEFAULT NULL,
  `issue_date` date NOT NULL,
  `other_baht` decimal(12,2) DEFAULT NULL,
  `rent_baht` decimal(12,2) DEFAULT NULL,
  `status` enum('OVERDUE','PAID','PENDING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `total_baht` decimal(12,2) DEFAULT NULL,
  `water_baht` decimal(12,2) DEFAULT NULL,
  `water_rate` decimal(12,2) DEFAULT NULL,
  `water_units` decimal(12,2) DEFAULT NULL,
  `room_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  `common_fee_baht` decimal(12,2) DEFAULT NULL,
  `garbage_fee_baht` decimal(12,2) DEFAULT NULL,
  `paid_date` date DEFAULT NULL,
  `maintenance_baht` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK938ysly43px2i4fwfsp3vjbat` (`room_id`),
  KEY `FKqddf0hbgcxful5wrvv0bhnyk0` (`tenant_id`),
  CONSTRAINT `FK938ysly43px2i4fwfsp3vjbat` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`),
  CONSTRAINT `FKqddf0hbgcxful5wrvv0bhnyk0` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `invoice`
--

LOCK TABLES `invoice` WRITE;
/*!40000 ALTER TABLE `invoice` DISABLE KEYS */;
INSERT INTO `invoice` VALUES (1,9,2025,'2025-09-08',960.00,8.00,120.00,'2025-09-01',50.00,7000.00,'PENDING',8100.00,90.00,18.00,5.00,1,2,NULL,NULL,NULL,NULL),(2,9,2025,'2025-09-08',720.00,8.00,90.00,'2025-09-01',50.00,8000.00,'PENDING',8842.00,72.00,18.00,4.00,2,2,NULL,NULL,NULL,NULL),(3,10,2025,'2025-09-08',800.00,8.00,100.00,'2025-09-01',50.00,8000.00,'PENDING',8904.00,54.00,18.00,3.00,2,2,NULL,NULL,NULL,NULL),(4,10,2025,'2025-09-09',800.00,8.00,100.00,'2025-09-02',50.00,7000.00,'PENDING',7904.00,54.00,18.00,3.00,1,2,0.00,0.00,NULL,NULL),(5,9,2025,'2025-09-09',800.00,8.00,100.00,'2025-09-02',50.00,7000.00,'PENDING',7904.00,54.00,18.00,3.00,1,2,0.00,0.00,NULL,NULL),(6,9,2025,'2025-09-09',800.00,8.00,100.00,'2025-09-02',50.00,7000.00,'PENDING',7904.00,54.00,18.00,3.00,1,2,0.00,0.00,NULL,NULL),(7,9,2025,'2025-09-09',800.00,8.00,100.00,'2025-09-02',50.00,7000.00,'PENDING',7904.00,54.00,18.00,3.00,1,2,0.00,0.00,NULL,NULL),(8,9,2025,'2025-09-09',960.00,8.00,120.00,'2025-09-02',50.00,7000.00,'PENDING',8010.00,0.00,0.00,0.00,1,2,0.00,0.00,NULL,NULL),(9,9,2025,'2025-09-09',0.00,0.00,0.00,'2025-09-02',0.00,7000.00,'PENDING',7090.00,90.00,18.00,5.00,1,2,0.00,0.00,NULL,NULL),(10,9,2025,'2025-09-09',800.00,8.00,100.00,'2025-09-02',50.00,7000.00,'PENDING',7904.00,54.00,18.00,3.00,1,2,0.00,0.00,NULL,NULL),(11,9,2025,'2025-09-09',960.00,8.00,120.00,'2025-09-02',50.00,7000.00,'PENDING',8064.00,54.00,18.00,3.00,1,2,0.00,0.00,NULL,NULL),(12,9,2025,'2025-09-14',960.00,8.00,120.00,'2025-09-07',0.00,7000.00,'PENDING',8364.00,54.00,18.00,3.00,1,2,0.00,0.00,NULL,350.00),(13,9,2025,'2025-09-11',1072.50,6.50,165.00,'2025-09-04',100.00,7000.00,'PENDING',8744.50,222.00,18.50,12.00,1,2,0.00,0.00,NULL,350.00),(14,9,2025,'2025-09-11',1072.50,6.50,165.00,'2025-09-04',100.00,7000.00,'PENDING',8744.50,222.00,18.50,12.00,1,2,0.00,0.00,NULL,350.00),(15,9,2025,'2025-09-11',999.99,NULL,NULL,'2025-09-04',0.00,7000.00,'PENDING',8572.21,222.22,NULL,NULL,1,2,0.00,0.00,NULL,350.00),(16,9,2025,'2025-09-11',999.99,NULL,NULL,'2025-09-04',0.00,7000.00,'PENDING',8572.21,222.22,NULL,NULL,1,2,0.00,0.00,NULL,350.00),(17,9,2025,'2025-09-11',NULL,NULL,NULL,'2025-09-04',0.00,6500.00,'PENDING',6850.00,NULL,NULL,NULL,1,2,0.00,0.00,NULL,350.00),(18,9,2025,'2025-09-11',NULL,NULL,NULL,'2025-09-04',0.00,6500.00,'PENDING',6850.00,NULL,NULL,NULL,1,2,0.00,0.00,NULL,350.00),(19,9,2025,'2025-09-11',1072.50,6.50,165.00,'2025-09-04',100.00,7000.00,'PENDING',8744.50,222.00,18.50,12.00,1,2,0.00,0.00,NULL,350.00),(20,9,2025,'2025-09-11',999.99,NULL,NULL,'2025-09-04',0.00,7000.00,'PENDING',8572.21,222.22,NULL,NULL,1,2,0.00,0.00,NULL,350.00),(21,9,2025,'2025-09-11',999.99,NULL,NULL,'2025-09-04',0.00,7000.00,'PENDING',8572.21,222.22,NULL,NULL,1,2,0.00,0.00,NULL,350.00),(22,9,2025,'2025-09-11',NULL,NULL,NULL,'2025-09-04',0.00,6500.00,'PENDING',6850.00,NULL,NULL,NULL,1,2,0.00,0.00,NULL,350.00),(23,9,2025,'2025-09-11',720.00,8.00,90.00,'2025-09-04',50.00,7000.00,'PENDING',8192.00,72.00,18.00,4.00,1,2,0.00,0.00,NULL,350.00),(24,9,2025,'2025-09-11',NULL,NULL,NULL,'2025-09-04',0.00,6800.00,'PENDING',7150.00,NULL,NULL,NULL,1,2,0.00,0.00,NULL,350.00),(25,9,2025,'2025-09-11',NULL,NULL,120.00,'2025-09-04',0.00,7000.00,'PENDING',7404.00,54.00,18.00,3.00,1,2,0.00,0.00,NULL,350.00),(26,10,2025,'2025-10-10',800.00,8.00,100.00,'2025-10-03',0.00,7000.00,'PENDING',7854.00,54.00,18.00,3.00,1,2,0.00,0.00,NULL,0.00),(27,9,2025,'2025-09-11',1072.50,6.50,165.00,'2025-09-04',100.00,7000.00,'PENDING',8744.50,222.00,18.50,12.00,1,2,0.00,0.00,NULL,350.00),(28,9,2025,'2025-09-11',999.99,NULL,NULL,'2025-09-04',0.00,7000.00,'PENDING',8572.21,222.22,NULL,NULL,1,2,0.00,0.00,NULL,350.00),(29,9,2025,'2025-09-11',NULL,NULL,NULL,'2025-09-04',0.00,6500.00,'PENDING',6850.00,NULL,NULL,NULL,1,2,0.00,0.00,NULL,350.00),(30,9,2025,'2025-09-11',720.00,8.00,90.00,'2025-09-04',50.00,7000.00,'PENDING',8192.00,72.00,18.00,4.00,1,2,0.00,0.00,NULL,350.00),(31,9,2025,'2025-09-11',800.00,8.00,100.00,'2025-09-04',50.00,7000.00,'PENDING',8254.00,54.00,18.00,3.00,1,2,0.00,0.00,NULL,350.00),(32,9,2025,'2025-09-11',800.00,8.00,100.00,'2025-09-04',50.00,7000.00,'PENDING',8254.00,54.00,18.00,3.00,1,2,0.00,0.00,NULL,350.00),(33,9,2025,'2025-09-16',1089.00,33.00,33.00,'2025-09-09',33.00,7000.00,'PENDING',10061.00,1089.00,33.00,33.00,1,2,0.00,0.00,NULL,850.00),(34,9,2025,'2025-09-16',484.00,22.00,22.00,'2025-09-09',3.00,7000.00,'PENDING',19326.00,10989.00,33.00,333.00,1,2,0.00,0.00,NULL,850.00);
/*!40000 ALTER TABLE `invoice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `lease`
--

DROP TABLE IF EXISTS `lease`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `lease` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `deposit_baht` decimal(12,2) DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `monthly_rent` decimal(12,2) DEFAULT NULL,
  `notes` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `start_date` date NOT NULL,
  `status` enum('ACTIVE','ENDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `room_id` bigint NOT NULL,
  `tenant_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKto1s8s57qp3gtqjiakvo240ke` (`room_id`),
  KEY `FKobm05vq3422sfratvn8x539l2` (`tenant_id`),
  CONSTRAINT `FKobm05vq3422sfratvn8x539l2` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`),
  CONSTRAINT `FKto1s8s57qp3gtqjiakvo240ke` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `lease`
--

LOCK TABLES `lease` WRITE;
/*!40000 ALTER TABLE `lease` DISABLE KEYS */;
INSERT INTO `lease` VALUES (1,14000.00,NULL,7000.00,'เริ่มสัญญา 1 ปี','2025-09-01','ACTIVE',1,2),(2,14000.00,NULL,7000.00,'เริ่มสัญญา 1 ปี','2025-09-01','ACTIVE',3,1);
/*!40000 ALTER TABLE `lease` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `maintenance`
--

DROP TABLE IF EXISTS `maintenance`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `maintenance` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `completed_date` date DEFAULT NULL,
  `cost_baht` decimal(12,2) DEFAULT NULL,
  `description` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `scheduled_date` date NOT NULL,
  `status` enum('CANCELED','COMPLETED','IN_PROGRESS','PLANNED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `room_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfoyu49hytdo3atyq3ak5uscd6` (`room_id`),
  CONSTRAINT `FKfoyu49hytdo3atyq3ak5uscd6` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `maintenance`
--

LOCK TABLES `maintenance` WRITE;
/*!40000 ALTER TABLE `maintenance` DISABLE KEYS */;
INSERT INTO `maintenance` VALUES (1,'2025-01-15',500.00,'Scheduled pest control','2025-01-10','COMPLETED',1),(2,'2025-09-04',500.00,'Scheduled pest control','2025-01-10','COMPLETED',1),(3,NULL,500.00,'Scheduled pest control','2025-01-10','PLANNED',1),(4,NULL,500.00,'Scheduled pest control','2025-01-10','PLANNED',1),(5,'2025-09-06',350.00,'ซ่อมก๊อกน้ำห้องน้ำ','2025-09-05','COMPLETED',1);
/*!40000 ALTER TABLE `maintenance` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `room`
--

DROP TABLE IF EXISTS `room`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `room` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `number` int DEFAULT NULL,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `tenant_id` bigint DEFAULT NULL,
  `common_fee_baht` decimal(12,2) DEFAULT NULL,
  `garbage_fee_baht` decimal(12,2) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKf844ewdfupww7gnjxinqrd4ly` (`tenant_id`),
  CONSTRAINT `FKf844ewdfupww7gnjxinqrd4ly` FOREIGN KEY (`tenant_id`) REFERENCES `tenant` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `room`
--

LOCK TABLES `room` WRITE;
/*!40000 ALTER TABLE `room` DISABLE KEYS */;
INSERT INTO `room` VALUES (1,101,'OCCUPIED',2,NULL,NULL),(2,102,'OCCUPIED',2,NULL,NULL),(3,201,'OCCUPIED',1,NULL,NULL);
/*!40000 ALTER TABLE `room` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `tenant`
--

DROP TABLE IF EXISTS `tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `line_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `tenant`
--

LOCK TABLES `tenant` WRITE;
/*!40000 ALTER TABLE `tenant` DISABLE KEYS */;
INSERT INTO `tenant` VALUES (1,'johnline','John Doe','080-123-4567'),(2,'jane_line','Jane Smith','081-222-3333');
/*!40000 ALTER TABLE `tenant` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-10  0:37:05
