// 생성일시 + 수정일시를 함께 제공하는 매핑 슈퍼클래스
package com.worksync.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

/** 생성일시 + 수정일시를 함께 제공하는 매핑 슈퍼클래스. */
@MappedSuperclass
@Getter
public abstract class BaseTimeEntity extends BaseEntity {

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
