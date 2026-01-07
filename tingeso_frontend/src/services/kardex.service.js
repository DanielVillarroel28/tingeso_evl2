import http from "../http-common";


const getMovements = (params) => {

    return http.get("/KARDEX-SERVICE/api/v1/kardex/", { params });
};

const kardexService = {
    getMovements,
};

export default kardexService;