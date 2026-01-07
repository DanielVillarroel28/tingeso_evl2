import httpClient from "../http-common";

const getAll = () => {
    return httpClient.get('/CLIENT-SERVICE/api/v1/clients');
}

const create = data => {
    return httpClient.post("/CLIENT-SERVICE/api/v1/clients", data);
}

const get = id => {
    return httpClient.get(`/CLIENT-SERVICE/api/v1/clients/${id}`);
}

const update = (id, data) => {
    return httpClient.put(`/CLIENT-SERVICE/api/v1/clients/${id}`, data);
}

const remove = id => {
    return httpClient.delete(`/CLIENT-SERVICE/api/v1/clients/${id}`);
}

const getMyProfile = () => {
    return httpClient.get('/CLIENT-SERVICE/api/v1/clients/me');
}


const updateMyProfile = data => {
    return httpClient.put('/CLIENT-SERVICE/api/v1/clients/me', data);
}
export default { getAll, create, get, update, remove, getMyProfile, updateMyProfile };