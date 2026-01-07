import httpClient from "../http-common";

const getAll = () => {
    return httpClient.get('/TOOL-SERVICE/api/v1/tools');
}

const create = data => {
    return httpClient.post("/TOOL-SERVICE/api/v1/tools", data);
}

const get = id => {
    return httpClient.get(`/TOOL-SERVICE/api/v1/tools/${id}`);
}

const update = (id, data) => {
    return httpClient.put(`/TOOL-SERVICE/api/v1/tools/${id}`, data);
}

const remove = id => {
    return httpClient.delete(`/TOOL-SERVICE/api/v1/tools/${id}`);
}

export default { getAll, create, get, update, remove };