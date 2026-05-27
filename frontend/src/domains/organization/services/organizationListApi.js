const BASE_URL = 'http://localhost:8080/api';

export async function getDepartments (accessToken) {
    return await fetch(`${BASE_URL}/departments`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`,
        },
    })
        .then((response) => {
            return response.json();
        })
        .then((json) => {
            console.log(json);
            return json;
        })
        .catch((error) => {
            console.log('에러발생: ' + error);
        })
}

export async function CreateDepartments (accessToken, newDeptName) {
    return await fetch(`${BASE_URL}/departments`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`,
        },
        body: JSON.stringify({name: newDeptName})
    })
        .then((response) => {
            return response.json();
        })
        .then((json) => {
            console.log(json);
            return json;
        })
        .catch((error) => {
            console.log('에러발생: ' + error);
        })
}

export async function getEmployee (accessToken) {
    return await fetch(`${BASE_URL}/employees`, {
        method: 'GET',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${accessToken}`,
        },
    })
        .then((response) => {
            return response.json();
        })
        .then((json) => {
            console.log(json);
            return json;
        })
        .catch((error) => {
            console.log('에러발생: ' + error);
        })
}

