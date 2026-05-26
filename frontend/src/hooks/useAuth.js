import { useState } from "react";

export default function useAuth() {
    // 토큰 저장
    const [accessToken, setAccessToken] = useState(null);

    // 로그인 (AccessToken 발급)
    const login = async (empNo, password) => {
        const response = await fetch('/api/auth/token', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({empNo, password})
        });
        const json = await response.json();
        if (!response.ok) {
            throw new Error(json.message || '로그인 실패');
        }
        setAccessToken(json.data.accessToken);
        localStorage.setItem('refreshToken', json.data.refreshToken);
    };

    // AccessToken 재발급
    const refresh = async () => {
        const refreshToken = localStorage.getItem('refreshToken');
        const response = await fetch('/api/auth/token/refresh', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({refreshToken})
        });
        const json = await response.json();
        if (!response.ok) {
            localStorage.removeItem('refreshToken');
            throw new Error(json.message || '재발급 실패');
        }
        setAccessToken(json.data.accessToken);
        localStorage.setItem('refreshToken', json.data.refreshToken);
        return json.data.accessToken;
    };

    // 로그아웃
    const logout = async () => {
        const refreshToken = localStorage.getItem('refreshToken')

        await fetch('/api/auth/token', {
            method: 'DELETE',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({refreshToken})
        })
            .then((response) => {
                return response.text();
            })
            .then((text) => {
                setAccessToken(null);
                localStorage.removeItem('refreshToken');
            })
            .catch((error) => {
                console.log("로그아웃 에러: " + error);
            })
    };

    return { accessToken, login, refresh, logout };
}