/**
 * @FileName : UserHome.js
 * @Description : 사용자 홈 피드
 *                - 날씨/뉴스/체크리스트 기반 홈 정보 제공
 *                - 주요 업무 메뉴 바로가기
 *                - 개인 온보딩 진행률 요약 및 추천 학습 이동 안내
 * @Author : 김다솜
 * @Date : 2026. 04. 21
 * @Modification_History
 * @
 * @ 수정일자        수정자        수정내용
 * @ ----------    ---------    -------------------------------
 * @ 2026.04.21    김다솜        최초 생성 및 홈 화면 구조 작성
 * @ 2026.04.22    김다솜        크롤링 로직 분리(Crawling.js) 및 위치 기반 날씨/기온별 배경색 연동, 홈 대시보드 레이아웃 전면 개편
 * @ 2026.05.08    김다솜        홈 화면 카드 배치 정리 및 업무 요약 영역 재구성
 * @ 2026.05.12    김다솜        홈 피드 업무 허브 구성, 날씨/뉴스/체크리스트 복구, To-Do 완료 표시 유지, 온보딩 요약/추천 학습 이동 및 전체화면 중앙 정렬 처리
 * @ 2026.05.14    김다솜        홈 피드 구조 개편, 위치 기반 날씨/기온별 배경색 연동, 홈 대시보드 레이아웃 전면 개편
 * @ 2026.05.15    김다솜        UI 조정 및 위치 기반 상세 날씨 위젯 적용
 * @ 2026.05.18    김다솜        날씨 위젯 현재 위치 구/동 단위 표시 반영
 * @ 2026.05.19    김다솜        사내 공지사항 및 커뮤니티 소식 영역 제거
 */

import React, { useEffect, useState } from 'react';
import {
  CCard,
  CCardBody,
  CCardHeader,
  CCol,
  CFormCheck,
  CProgress,
  CProgressBar,
  CRow,
} from '@coreui/react';
import CIcon from '@coreui/icons-react';
import { cilCheckCircle, cilSun, cilCloud, cilRain, cilSnowflake, cilHappy, cilSpeaker } from '@coreui/icons';
import { useNavigate } from 'react-router-dom';
import axiosInstance from 'src/api/axiosInstance';
import { useUser } from 'src/api/UserContext';
import { PATH } from 'src/constants/path';
import { userHomePageStyle, userWelcomeSection, progressLabel } from 'src/styles/js/common/UserHomeStyle';
import { badgeAiTag, cardCore, COLORS } from 'src/styles/js/onboarding/OnboardingStyle';
import { request } from 'src/helpers/axios_helper';
import { fetchHomeData } from './Crawling';

const buildHomeFeedData = (dashboardData, roadmapGroups = []) => {
  const categoryProgresses = dashboardData?.categoryProgresses || [];
  const incompleteCategories = categoryProgresses.filter((category) => category.progressPercent < 100);

  const recommendations = roadmapGroups
    .flatMap((group) =>
      (group.items || [])
        .filter((item) => item.status !== 'COMPLETED')
        .map((item) => ({
          title: item.item_title || item.title || group.category_name,
          type: group.category_name,
          contentId: item.content_id || item.contentId,
          itemId: item.item_id || item.itemId,
          categoryName: group.category_name,
        }))
    )
    .slice(0, 3);

  return {
    totalProgress: dashboardData?.learningProgressPercent ?? 0,
    checklist: {
      completed: dashboardData?.completedChecklistCount ?? 0,
      total: dashboardData?.totalChecklistCount ?? 0,
    },
    evaluation: {
      passRate: dashboardData?.evaluationPassRatePercent ?? 0,
    },
    aiCoaching: incompleteCategories.length,
    firstIncompleteCategory: incompleteCategories[0]?.categoryName || recommendations[0]?.categoryName,
    recommendations,
  };
};

const UserHome = () => {
  const { userInfo } = useUser();
  const navigate = useNavigate();
  const [dashboardData, setDashboardData] = useState(null);
  const [roadmapGroups, setRoadmapGroups] = useState([]);
  const [todoList, setTodoList] = useState([]);
  const [personalGreetingMessage, setPersonalGreetingMessage] = useState(''); // 시간대별 인사말만 저장
  const [homeData, setHomeData] = useState({
    temp: '--',
    desc: '로딩 중...',
    icon: cilSun,
    city: 'Seoul',
    feelsLike: null,
    humidity: null,
    windSpeed: null,
    cloudiness: null,
    locationSource: '',
    newsList: [],
    encouragement: '',
  });

  useEffect(() => {
    const initHomeData = async () => {
      try {
        // 1. 뉴스 데이터는 기존 Crawling 유지
        const crawlData = await fetchHomeData();
        
        // 2. 시간대별 맞춤 인사말 생성 (랜덤성 추가로 단조로움 해소)
        const hour = new Date().getHours();
        const greetingPool = {
          morning: ['좋은 아침이에요!', '활기찬 아침입니다!', '오늘도 힘차게 시작해봐요!'],
          afternoon: ['즐거운 오후예요!', '나른한 오후, 조금만 더 힘내세요!', '오후 업무도 화이팅입니다!'],
          evening: ['편안한 저녁 되세요!', '오늘 하루 고생 많으셨어요!', '차분한 저녁 시간 보내세요.'],
          night: ['오늘 하루도 고생 많으셨어요!', '내일을 위해 푹 쉬세요.', '고요한 밤입니다.']
        };

        let pool = ['반가워요!'];
        if (hour >= 5 && hour < 12) pool = greetingPool.morning;
        else if (hour >= 12 && hour < 18) pool = greetingPool.afternoon;
        else if (hour >= 18 && hour < 22) pool = greetingPool.evening;
        else pool = greetingPool.night;

        const timeGreeting = pool[Math.floor(Math.random() * pool.length)];
        setPersonalGreetingMessage(timeGreeting); // 인사말 메시지만 저장

        // 3. Geolocation API를 이용한 위치 기반 날씨 호출
        if (navigator.geolocation) {
          navigator.geolocation.getCurrentPosition(async (position) => {
            try {
              const params = {
                lat: position.coords.latitude,
                lon: position.coords.longitude,
              };
              const res = await request('GET', '/api/weather', params);
              const weather = res.data;
              
              // OpenWeatherMap의 날씨 상태(Main)를 CoreUI 아이콘으로 매칭
              const mainStatus = weather.weather[0].main;
              let weatherIcon = cilSun;
              if (mainStatus.includes('Clouds')) weatherIcon = cilCloud;
              else if (mainStatus.includes('Rain') || mainStatus.includes('Drizzle')) weatherIcon = cilRain;
              else if (mainStatus.includes('Snow')) weatherIcon = cilSnowflake;

              setHomeData({
                temp: weather.main?.temp?.toFixed(1) ?? '--',
                feelsLike: weather.main?.feels_like?.toFixed(1) ?? null,
                humidity: weather.main?.humidity ?? null,
                windSpeed: weather.wind?.speed?.toFixed(1) ?? null,
                cloudiness: weather.clouds?.all ?? null,
                desc: weather.weather?.[0]?.description ?? '날씨 정보 없음',
                icon: weatherIcon,
                city: weather.display_location_detail || weather.display_location || weather.name,
                locationSource: weather.location_source === 'GPS' ? '현재 위치 기준' : '',
                newsList: crawlData.newsList || [],
                encouragement: weather.encouragement_message,
              });
            } catch (error) {
              console.error('[UserHome] 위치 기반 날씨 로드 실패:', error);
              // 실패 시 서울 기본값 유지 (Crawling 데이터 활용)
              setHomeData(prev => ({ ...prev, ...crawlData, newsList: crawlData.newsList }));
            }
          }, (err) => {
            console.warn('[UserHome] 위치 권한 거부:', err);
            setHomeData(prev => ({ ...prev, ...crawlData, newsList: crawlData.newsList }));
          });
        } else {
          setHomeData(prev => ({ ...prev, ...crawlData, newsList: crawlData.newsList }));
        }
      } catch (error) {
        console.error('[UserHome] 날씨/뉴스 데이터 로드 실패:', error);
      }
    };

    initHomeData();
  }, [userInfo]);

  useEffect(() => {
    const fetchOnboardingData = async () => {
      const empNo = userInfo?.empNo || userInfo?.emp_no;
      if (!empNo) return;

      try {
        const [dashboardResponse, roadmapResponse, checklistResponse] = await Promise.all([
          axiosInstance.get(PATH.API.ONBOARDING.DASHBOARD(empNo)),
          axiosInstance.get(PATH.API.ONBOARDING.ROADMAP(empNo)),
          axiosInstance.get(PATH.API.ONBOARDING.CHECKLIST_LIST(empNo)),
        ]);

        const todos = (checklistResponse.data || [])
          .filter((item) => item.status !== 'COMPLETED')
          .sort((a, b) => {
            if (Boolean(a.isMandatory) !== Boolean(b.isMandatory)) {
              return a.isMandatory ? -1 : 1;
            }
            return (a.orderNo || 0) - (b.orderNo || 0);
          })
          .slice(0, 3);

        setDashboardData(dashboardResponse.data);
        setRoadmapGroups(roadmapResponse.data?.recommended_roadmap || []);
        setTodoList(todos);
      } catch (error) {
        console.error('[UserHome] 온보딩 홈 피드 데이터 로드 실패:', error);
      }
    };

    fetchOnboardingData();
  }, [userInfo?.empNo, userInfo?.emp_no]);

  const data = buildHomeFeedData(dashboardData, roadmapGroups);

  // 기온에 따른 날씨 위젯 배경색 동적 처리 함수
  const getWeatherCardStyle = (temp) => {
    const t = parseFloat(temp);
    let gradient = 'linear-gradient(135deg, #3399ff 0%, #1f2a56 100%)'; // 기본 (보통)

    if (!isNaN(t)) {
      if (t <= 5) gradient = 'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)';      // 추움 (Cyan/Light Blue)
      else if (t > 5 && t <= 20) gradient = 'linear-gradient(135deg, #3399ff 0%, #1f2a56 100%)'; // 선선 (Blue)
      else if (t > 20 && t <= 28) gradient = 'linear-gradient(135deg, #f6d365 0%, #fda085 100%)'; // 따뜻 (Yellow/Orange)
      else if (t > 28) gradient = 'linear-gradient(135deg, #ff0844 0%, #ffb199 100%)'; // 더움 (Red/Pink)
    }

    return {
      background: gradient,
      minHeight: '190px',
      transition: 'background 0.5s ease', // 색상 변경 시 부드럽게 전환
    };
  };

  const goRoadmapFocus = (categoryName = data.firstIncompleteCategory) => {
    navigate(PATH.ONBOARDING.ROADMAP, {
      state: categoryName ? { focusCategory: categoryName } : {},
    });
  };

  const goLearning = (recommendation) => {
    if (recommendation.contentId) {
      navigate(PATH.ONBOARDING.LEARNING(recommendation.contentId), {
        state: {
          title: recommendation.title,
          itemId: recommendation.itemId,
          userInfo,
        },
      });
      return;
    }

    goRoadmapFocus(recommendation.categoryName);
  };

  const handleTodoToggle = async (item) => {
    const empNo = userInfo?.empNo || userInfo?.emp_no;
    if (!empNo) return;

    const isCurrentlyCompleted = item.status === 'COMPLETED';
    const apiPath = isCurrentlyCompleted 
      ? PATH.API.ONBOARDING.CHECKLIST_UNCOMPLETE 
      : PATH.API.ONBOARDING.CHECKLIST_COMPLETE;

    try {
      await axiosInstance.post(apiPath, {
        empNo,
        checklistId: item.checklistId,
      });

      setTodoList((prev) =>
        prev.map((todo) =>
          todo.checklistId === item.checklistId 
            ? { ...todo, status: isCurrentlyCompleted ? 'NOT_STARTED' : 'COMPLETED' } 
            : todo
        )
      );
      window.dispatchEvent(new Event('onboardingProgressUpdated'));
    } catch (error) {
      console.error('[UserHome] 홈 체크리스트 상태 변경 실패:', error);
    }
  };

  return (
    <div style={userHomePageStyle}>
      <div style={userWelcomeSection}>
        <h2 className="fw-bold mb-2" style={{ lineHeight: '1.4' }}>
          {userInfo?.name || '사용자'} 님!
          <CIcon icon={cilHappy} className="ms-2" style={{ color: '#ffc107', verticalAlign: 'middle' }} height={24} />
          <br />
          <span style={{ fontSize: '0.85em', fontWeight: '500', opacity: 0.9 }}>
            {homeData.encouragement || personalGreetingMessage}
          </span>
        </h2>
        <p className="mb-0 opacity-75">
          오늘의 날씨와 사내 소식을 확인하고, 남은 온보딩 학습을 이어가 보세요.
        </p>
      </div>

      {/* 1열: 날씨 & 뉴스 */}
      <CRow className="justify-content-center mb-4">
        {/* 날씨 위젯 */}
        <CCol lg={4} className="mb-3 mb-lg-0">
          <CCard
            className="h-100 border-0 shadow-sm text-white"
            style={getWeatherCardStyle(homeData.temp)}
          >
            <CCardBody className="p-3 d-flex flex-column justify-content-center">
              <div className="d-flex justify-content-between align-items-start gap-3">
                <div>
                  <div className="small fw-semibold" style={{ opacity: 0.85 }}>
                    {homeData.locationSource || '기본 위치 기준'}
                  </div>
                  <div className="small fw-medium mt-1" style={{ opacity: 0.9 }}>
                    {homeData.city} · {homeData.desc}
                  </div>
                </div>
                <CIcon icon={homeData.icon || cilSun} height={42} />
              </div>

              <div className="fs-1 fw-bold mt-3">{homeData.temp}°C</div>

              <div
                className="d-grid mt-2"
                style={{
                  gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
                  gap: '8px',
                  fontSize: '0.78rem',
                }}
              >
                <div className="p-2 rounded" style={{ backgroundColor: 'rgba(255,255,255,0.14)' }}>
                  <div style={{ opacity: 0.75 }}>체감</div>
                  <strong>{homeData.feelsLike !== null ? `${homeData.feelsLike}°C` : '-'}</strong>
                </div>
                <div className="p-2 rounded" style={{ backgroundColor: 'rgba(255,255,255,0.14)' }}>
                  <div style={{ opacity: 0.75 }}>습도</div>
                  <strong>{homeData.humidity !== null ? `${homeData.humidity}%` : '-'}</strong>
                </div>
                <div className="p-2 rounded" style={{ backgroundColor: 'rgba(255,255,255,0.14)' }}>
                  <div style={{ opacity: 0.75 }}>풍속</div>
                  <strong>{homeData.windSpeed !== null ? `${homeData.windSpeed}m/s` : '-'}</strong>
                </div>
                <div className="p-2 rounded" style={{ backgroundColor: 'rgba(255,255,255,0.14)' }}>
                  <div style={{ opacity: 0.75 }}>구름</div>
                  <strong>{homeData.cloudiness !== null ? `${homeData.cloudiness}%` : '-'}</strong>
                </div>
              </div>

              {homeData.encouragement && (
                <div className="p-2 rounded w-100 mt-2" style={{ backgroundColor: 'rgba(255,255,255,0.15)', fontSize: '0.85rem' }}>
                  {homeData.encouragement}
                </div>
              )}
            </CCardBody>
          </CCard>
        </CCol>

        {/* 뉴스 플래시 */}
        <CCol lg={8}>
          <CCard className="h-100" style={cardCore}>
            <CCardHeader className="bg-white border-0 py-3 d-flex align-items-center">
              <CIcon icon={cilSpeaker} className="me-2 text-info" />
              <strong className="text-dark" style={{ fontSize: '1rem' }}>IT NEWS FLASH</strong>
            </CCardHeader>
            <CCardBody className="pt-0">
              <div className="row">
                {(homeData.newsList || []).slice(0, 4).map((item, index) => (
                  <div key={`${item.title}-${index}`} className="col-md-6 py-2 border-bottom">
                    <a
                      href={item.link}
                      target="_blank"
                      rel="noreferrer"
                      className="text-decoration-none text-dark small d-block text-truncate"
                    >
                      • {item.title}
                    </a>
                  </div>
                ))}
              </div>
              <div className="mt-2 text-end">
                <small className="text-muted" style={{ fontSize: '0.7rem' }}>출처: 네이버 뉴스</small>
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>

      {/* 2열: 온보딩 진행 요약, 투두리스트, 다음 추천 학습 */}
      <CRow className="justify-content-center">
        {/* 온보딩 요약 */}
        <CCol lg={4} className="mb-4">
          <CCard className="h-100" style={cardCore}>
            <CCardHeader className="bg-white border-0 py-3 d-flex justify-content-between align-items-center">
              <h6 className="mb-0 fw-bold text-dark">
                온보딩 요약 <span className="badge ms-1" style={{...badgeAiTag, fontSize: '0.6rem'}}>AI</span>
              </h6>
              <small
                style={{ cursor: 'pointer', color: COLORS.primary, fontWeight: 700 }}
                onClick={() => goRoadmapFocus()}
              >
                전체 보기
              </small>
            </CCardHeader>
            <CCardBody className="py-3">
              <div className="d-flex justify-content-between align-items-end mb-2">
                <span style={progressLabel}>전체 학습 진행률</span>
                <span className="h5 mb-0 fw-bold" style={{ color: COLORS.primary }}>{data.totalProgress}%</span>
              </div>
              <CProgress height={8} className="bg-light mb-4">
                <CProgressBar style={{ backgroundColor: COLORS.primary }} value={data.totalProgress} animated={data.totalProgress < 100} />
              </CProgress>

              <CRow>
                <CCol xs={6} className="mb-3">
                  <div className="small text-muted">체크리스트</div>
                  <div className="fw-bold text-dark">{data.checklist.completed} / {data.checklist.total}</div>
                </CCol>
                <CCol xs={6} className="mb-3">
                  <div className="small text-muted">평가 통과율</div>
                  <div className="fw-bold text-dark">{data.evaluation.passRate}%</div>
                </CCol>
              </CRow>
            </CCardBody>
          </CCard>
        </CCol>

        {/* 투두리스트 */}
        <CCol lg={4} className="mb-4">
          <CCard className="h-100" style={cardCore}>
            <CCardHeader className="bg-white border-0 py-3 d-flex justify-content-between align-items-center">
              <div>
                <CIcon icon={cilCheckCircle} className="me-2 text-success" />
                <strong>To-Do List</strong>
              </div>
              <small
                style={{ cursor: 'pointer', color: COLORS.primary, fontWeight: 700 }}
                onClick={() => navigate(PATH.ONBOARDING.CHECKLIST)}
              >
                전체 보기
              </small>
            </CCardHeader>
            <CCardBody>
              {todoList.length === 0 ? (
                <div className="text-muted small">남은 체크리스트가 없습니다.</div>
              ) : (
                todoList.map((item) => {
                  const isCompleted = item.status === 'COMPLETED';

                  return (
                  <div
                    key={item.checklistId}
                    className="mb-3"
                    style={{
                      opacity: isCompleted ? 0.65 : 1,
                      transition: 'opacity 0.2s ease',
                    }}
                  >
                    <CFormCheck
                      checked={isCompleted}
                      label={
                        <small
                          style={{
                            textDecoration: isCompleted ? 'line-through' : 'none',
                            color: isCompleted ? '#6c757d' : 'inherit',
                          }}
                        >
                          {item.title}
                        </small>
                      }
                      onChange={() => handleTodoToggle(item)}
                    />
                    <div className="text-muted" style={{ fontSize: '11px', paddingLeft: '24px' }}>
                      {item.category} · {item.isMandatory ? '필수' : '선택'}
                    </div>
                  </div>
                  );
                })
              )}
            </CCardBody>
          </CCard>
        </CCol>

        {/* 다음 추천 학습 */}
        <CCol lg={4} className="mb-4">
          <CCard className="h-100" style={cardCore}>
            <CCardHeader className="bg-white border-0 py-3">
              <h6 className="mb-0 fw-bold text-dark">다음 추천 학습</h6>
            </CCardHeader>
            <CCardBody className="p-0">
              <div className="list-group list-group-flush">
                {data.recommendations.length === 0 ? (
                  <div className="list-group-item px-4 py-3 text-muted small">
                    현재 이어갈 추천 학습이 없습니다.
                  </div>
                ) : (
                  data.recommendations.map((rec, idx) => (
                    <button
                      key={`${rec.contentId || rec.categoryName}-${idx}`}
                      type="button"
                      className="list-group-item list-group-item-action px-4 py-3 border-0"
                      style={{
                        borderBottom: idx === data.recommendations.length - 1 ? 'none' : `1px solid ${COLORS.border}`,
                        textAlign: 'left',
                      }}
                      onClick={() => goLearning(rec)}
                    >
                      <div className="small text-muted mb-1">{rec.type}</div>
                      <div className="fw-bold text-dark">{rec.title}</div>
                    </button>
                  ))
                )}
              </div>
            </CCardBody>
          </CCard>
        </CCol>
      </CRow>
    </div>
  );
};

export default UserHome;
