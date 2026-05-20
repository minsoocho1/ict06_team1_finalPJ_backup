import { configureStore, createSlice } from '@reduxjs/toolkit'

//1. 슬라이스(Slice) 생성: 상태와 변경 규칙을 한 번에 정의
const sidebarSlice = createSlice({
    //초기 상태 설정(사이드바 열림 상태: true)
    name: 'sidebar',
    initialState: {
        sidebarShow: true,
        sidebarUnfoldable: false,
    },
    //상태 변경 규칙(Reducer) 정의
    reducers: {
        set: (state, action) => {
            return { ...state, ...action.payload }
        },
    },
})

export const { set: setSidebarState } = sidebarSlice.actions

//2. 스토어 생성
const store = configureStore({
    reducer: sidebarSlice.reducer,
})

export default store;
