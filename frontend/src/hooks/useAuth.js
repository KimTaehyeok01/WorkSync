import { useState } from "react";

export default function useAuth() {
    // 토큰 저장
    const [accessToken, setAccessToken] = useState(null);

    // 로그인 (AccessToken 발급)
    const login = async (empNo, password) => {
        await fetch('/api/auth/token', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({empNo, password})
        })
            .then((response) => {
                return response.json();
            })
            .then((json) => {
                setAccessToken(json.accessToken);
                localStorage.setItem('refreshToken', json.refreshToken);    
            })
            .catch((error) => {
                console.log("로그인 에러: " + error);
            })
    };

    // AccessToken 재발급
    const refresh = async () => {
        const refreshToken = localStorage.getItem('refreshToken')

        await fetch('/api/auth/token/refresh', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({refreshToken})
        })
            .then((response) => {
                return response.json();
            })
            .then((json) => {
                setAccessToken(json.accessToken);
                localStorage.setItem('refreshToken', json.refreshToken);
            })
            .catch((error) => {
                console.log("재발급 에러: " + error);
            })
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