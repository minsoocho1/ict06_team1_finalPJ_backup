/**
 * @FileName : RepositoryConfig.java
 * @Description : JPA Repository 인식 범위 설정 클래스
 * @Author : 김다솜
 * @Date : 2026. 05. 19
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.19    김다솜        최초 생성 및 JPA Repository 인식 범위 명시
 */
package com.ict06.team1_fin_pj.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
// JPA Repository 위치를 명시해 Spring Data JPA가 해당 Repository만 스캔하도록 설정한다.
@EnableJpaRepositories(basePackages = "com.ict06.team1_fin_pj")
public class RepositoryConfig {
}
