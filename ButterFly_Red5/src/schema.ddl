CREATE DATABASE  IF NOT EXISTS `butterflydb` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `butterflydb`;
-- MySQL dump 10.13  Distrib 5.5.37, for debian-linux-gnu (x86_64)
--
-- Host: localhost    Database: butterflydb
-- ------------------------------------------------------
-- Server version	5.5.37-0ubuntu0.12.04.1-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `gcm_user_mails`
--

DROP TABLE IF EXISTS `gcm_user_mails`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gcm_user_mails` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `mail` varchar(45) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `mail` (`mail`),
  UNIQUE KEY `mail_2` (`mail`),
  KEY `FK175F1E16F21DFF8` (`user_id`),
  CONSTRAINT `FK175F1E16F21DFF8` FOREIGN KEY (`user_id`) REFERENCES `gcm_users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=188 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gcm_user_mails`
--

LOCK TABLES `gcm_user_mails` WRITE;
/*!40000 ALTER TABLE `gcm_user_mails` DISABLE KEYS */;
INSERT INTO `gcm_user_mails` VALUES (185,'mail@mail.com',180),(186,'ahmetmermerkaya@gmail.com',181),(187,'ahmetmermerkaya@hotmail.com',181);
/*!40000 ALTER TABLE `gcm_user_mails` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stream_viewers`
--

DROP TABLE IF EXISTS `stream_viewers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `stream_viewers` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `userId` int(11) DEFAULT NULL,
  `streamId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FKDF086E2563426DC` (`streamId`),
  KEY `FKDF086E2E612240F` (`userId`),
  CONSTRAINT `FKDF086E2563426DC` FOREIGN KEY (`streamId`) REFERENCES `streams` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FKDF086E2E612240F` FOREIGN KEY (`userId`) REFERENCES `gcm_users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=86 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stream_viewers`
--

LOCK TABLES `stream_viewers` WRITE;
/*!40000 ALTER TABLE `stream_viewers` DISABLE KEYS */;
INSERT INTO `stream_viewers` VALUES (84,181,246),(85,181,248);
/*!40000 ALTER TABLE `stream_viewers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `gcm_users`
--

DROP TABLE IF EXISTS `gcm_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gcm_users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=182 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gcm_users`
--

LOCK TABLES `gcm_users` WRITE;
/*!40000 ALTER TABLE `gcm_users` DISABLE KEYS */;
INSERT INTO `gcm_users` VALUES (180),(181);
/*!40000 ALTER TABLE `gcm_users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `streams`
--

DROP TABLE IF EXISTS `streams`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `streams` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `altitude` double DEFAULT NULL,
  `user_id` int(11) NOT NULL,
  `isLive` tinyint(1) DEFAULT NULL,
  `isPublic` tinyint(1) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `registerTime` datetime NOT NULL,
  `streamName` varchar(255) NOT NULL,
  `streamUrl` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK_USER_STREAM` (`user_id`),
  CONSTRAINT `FK_USER_STREAM` FOREIGN KEY (`user_id`) REFERENCES `gcm_users` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB AUTO_INCREMENT=249 DEFAULT CHARSET=utf8;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `streams`
--

LOCK TABLES `streams` WRITE;
/*!40000 ALTER TABLE `streams` DISABLE KEYS */;
INSERT INTO `streams` VALUES (246,NULL,180,1,0,NULL,NULL,'2014-05-24 13:38:09','publishedName','publishUrl1'),(247,NULL,180,1,1,NULL,NULL,'2014-05-24 13:38:09','publishedNamesdfdsf','publishUrl1sdsdfs'),(248,NULL,180,1,1,NULL,NULL,'2014-05-24 13:38:09','publishedsfsfNamesdfdsf','publishUrl1sdsdfs32424');
/*!40000 ALTER TABLE `streams` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reg_ids`
--

DROP TABLE IF EXISTS `reg_ids`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `reg_ids` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `gcm_reg_id` varchar(255) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `FK40B8148DF21DFF8` (`user_id`),
  CONSTRAINT `FK40B8148DF21DFF8` FOREIGN KEY (`user_id`) REFERENCES `gcm_users` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=206 DEFAULT CHARSET=latin1;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reg_ids`
--

LOCK TABLES `reg_ids` WRITE;
/*!40000 ALTER TABLE `reg_ids` DISABLE KEYS */;
INSERT INTO `reg_ids` VALUES (202,'22',180),(203,'deneme',181),(204,'deneme123131',181),(205,'APA91bFb_756VSgDdQqtd0Rw5opG73rIOwUYHdxjTRlMDOSql8HFLENGWm_RSFCcs-kT5opHs6fsanTvjW0G9ocJMcmbW2zUci6eUiC8U64fs0T086RhOmlUvz6691S4CSAn4AmYI_6RyiVYzZ6g2xBFrbXi7T-8Lg',181);
/*!40000 ALTER TABLE `reg_ids` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2014-05-25 17:16:20
