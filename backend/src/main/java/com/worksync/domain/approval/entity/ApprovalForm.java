package com.worksync.domain.approval.entity;

import com.worksync.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "approval_form")
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApprovalForm extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "form_name", nullable = false, length = 100)
    private String formName;

    @Column(name = "form_type", nullable = false, length = 50)
    private String formType;

    @Column(name = "form_schema", nullable = false, columnDefinition = "jsonb")
    private String formSchema;

}
