import { createContext, useContext, useState, useEffect } from 'react';
import useAuth from '../hooks/useAuth';

// createContext = 빈 보관함 생성
// AuthContext.Provider = 값을 넣는 입구
// AuthContext.Consumer = 값을 꺼내는 출구, useContext(AuthContext) 대체 가능
const AuthContext = createContext(null);

// Router에서 <AuthProvider></AuthProvider> 안에 넣은 모든 컴포넌트 전달
export function AuthProvider({ children }) {
    const { accessToken, login, refresh, logout } = useAuth();
    const [isLoading, setIsLoading] = useState(true);

    // 인증 헤더 토큰 추가
    const authFetch = async (url, options = {}) => {
        return await fetch(url, {
            method: options.method || 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${accessToken}`,
                ...options.headers
            },
            body: options.body
        })
            .then((response) => {
                if (response.status === 401) {
                    return refresh().then((newAccessToken) => {
                        return fetch(url, {
                            method: options.method || 'GET',
                            headers: {
                                'Content-Type': 'application/json',
                                'Authorization': `Bearer ${newAccessToken}`,
                                ...options.headers
                            },
                            body: options.body
                        })
                    })
                }
                return response;
            })
            .catch((error) => {
                console.log("인증 에러: " + error);
            })
    }

    // 앱 시작 후 전에 로그인 상태 확인 (refreshToken 있는지 확인)
    useEffect(() => {
        const loginCheck = async () => {
            const refreshToken = localStorage.getItem('refreshToken');
            if (refreshToken) {
                try {
                    await refresh();
                } catch (e) {
                    // 만료된 refreshToken — 이미 refresh() 내부에서 localStorage 제거됨
                }
            }
            setIsLoading(false);
        };
        
        loginCheck();
    }, []);

    const authValue = { 
        accessToken, 
        isAuthenticated: accessToken ? true : false,
        isLoading,
        login, 
        refresh, 
        logout,
        authFetch,
    };

    return (
        <AuthContext.Provider value={authValue}>
            {children}
        </AuthContext.Provider>
    );
}

export default function useAuthContext() {
    return useContext(AuthContext);
}