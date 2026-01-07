import http from "../http-common";

const getAll = () => {
    return http.get("/FINE-SERVICE/api/v1/fines"); 
};

const pay = (id) => {
    return http.put(`/FINE-SERVICE/api/v1/fines/${id}/pay`);
};

const getMyFines = () => {
    return http.get("/FINE-SERVICE/api/v1/fines/my-fines");
};

const fineService = {
    getAll,
    pay,
    getMyFines,
};

export default fineService;