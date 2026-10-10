// 알림 수신 설정 Repository
package com.worksync.domain.notification.repository;

import com.worksync.domain.notification.entity.NotificationSetting;
import com.worksync.domain.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 알림 수신 설정 Repository. */
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

	List<NotificationSetting> findByEmployeeId(Long employeeId);

	Optional<NotificationSetting> findByEmployeeIdAndType(Long employeeId, NotificationType type);

	// 해당 유형 알림을 꺼 두었는지
	boolean existsByEmployeeIdAndTypeAndIsEnabledFalse(Long employeeId, NotificationType type);
}
