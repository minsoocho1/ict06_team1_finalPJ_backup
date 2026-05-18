/**
 * @FileName : axiosInstance.js
 * @Description : Spring Boot API 공통 Axios 인스턴스 설정
 * @Author : 김다솜
 * @Date : 2026. 05. 01
 * @Modification_History
 * @
 * @ 수정일        수정자       수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.05.01    김다솜        최초 생성 및 JWT 자동 헤더 처리 추가
 * @ 2026.05.07    김다솜        401 응답 시 Refresh Token 기반 재발급 처리 추가
 * @ 2026.05.14    김다솜        JWT 만료/재발급 테스트용 프론트 로그 보강
 */

import axios from 'axios';
import { PATH } from 'src/constants/path';

const axiosInstance = axios.create({
  baseURL: PATH.API.BASE,
});

axiosInstance.interceptors.request.use(
  (config) => {
    const accessToken = localStorage.getItem('accessToken');

    if (accessToken) {
      console.log(`[Axios Request] ${config.method?.toUpperCase()} ${config.url} (Token Included)`);
      config.headers.Authorization = `Bearer ${accessToken}`;
    } else {
      console.log(`[Axios Request] ${config.method?.toUpperCase()} ${config.url} (No Token)`);
    }

    return config;
  },
  (error) => Promise.reject(error),
);

axiosInstance.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const requestUrl = originalRequest?.url || '';
    const isAuthEndpoint = requestUrl.includes('/auth/login') || requestUrl.includes('/auth/refresh');

    if (error.response?.status === 401 && !originalRequest?._retry && !isAuthEndpoint) {
      console.warn(`[Axios Interceptor] 401 감지 - 원본 요청: ${originalRequest?.method?.toUpperCase()} ${requestUrl}`);
      console.warn('[Axios Interceptor] Access Token 만료 가능성 확인 - Refresh Token 재발급 시도');
      originalRequest._retry = true;
      const refreshToken = localStorage.getItem('refreshToken');

      if (refreshToken) {
        try {
          console.log(`[Axios Interceptor] Refresh 요청 시작 - 토큰 길이: ${refreshToken.length}`);

          const res = await axios.post(`${PATH.API.BASE}/auth/refresh`, { refreshToken });
          const { accessToken: newAccessToken } = res.data;

          localStorage.setItem('accessToken', newAccessToken);
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

          console.log(`[Axios Interceptor] Refresh 성공 - 새 Access Token 길이: ${newAccessToken.length}`);
          console.log(`[Axios Interceptor] 원본 요청 재시도 - ${originalRequest?.method?.toUpperCase()} ${requestUrl}`);

          return axiosInstance(originalRequest);
        } catch (refreshError) {
          console.error('[Axios Interceptor] Refresh 실패 - 재로그인 필요');
          console.error('[Axios Interceptor] 401 원본 요청 URL:', requestUrl);
          console.error('[Axios Interceptor] Refresh 실패 응답:', refreshError?.response?.data || refreshError.message);

          localStorage.removeItem('accessToken');
          localStorage.removeItem('refreshToken');
          return Promise.reject(refreshError);
        }
      } else {
        console.warn('[Axios Interceptor] Refresh Token 없음 - 자동 재발급 불가');
      }
    }

    return Promise.reject(error);
  },
);

export default axiosInstance;
