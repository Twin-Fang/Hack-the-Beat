package kr.suhsaechan.hackthebeat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 전면 개방. 인증·권한이 없는 공개 API라 오리진·메서드·헤더를 제한하지 않는다.
 * 어느 오리진에서 붙어도 막히지 않게 하는 것이 목적이라 자격증명(쿠키)만 끈다
 * (allowCredentials=true 면 와일드카드 오리진을 쓸 수 없다).
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
