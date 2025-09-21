package com.epam.gymcrm.db.repository;

import com.epam.gymcrm.db.entity.TrainingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TrainingRepository extends JpaRepository<TrainingEntity, Long>, JpaSpecificationExecutor<TrainingEntity> {
    Optional<TrainingEntity> findByTrainerIdAndTrainingDate(Long trainerId, LocalDateTime trainingDate);

    Optional<TrainingEntity> findByTrainer_User_UsernameAndTrainingDate(String trainingUsername, LocalDateTime trainingDate);

    @Modifying
    @Query("""
                delete from TrainingEntity t
                where t.trainer.user.username = :trainer and t.trainingDate = :date
            """)
    int deleteByTrainerAndDate(@Param("trainer") String trainerUsername,
                               @Param("date") LocalDateTime trainingDate);
}
