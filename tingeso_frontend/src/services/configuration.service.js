import http from "../http-common";

const getLateFee = () => {
    return http.get("/FINE-SERVICE/config/late-fee");
};

const updateLateFee = (data) => {
    return http.put("/FINE-SERVICE/config/late-fee", data);
};

const getRepairFee = () => {
    return http.get("/FINE-SERVICE/config/repair-fee");
};

const updateRepairFee = (data) => {
    return http.put("/FINE-SERVICE/config/repair-fee", data);
};

const getRentalFee = () => {
    return http.get("/FINE-SERVICE/config/rental-fee");
};

const updateRentalFee = (data) => {
    return http.put("/FINE-SERVICE/config/rental-fee", data);
};

const configurationService = {
    getLateFee,
    updateLateFee,
    getRepairFee,
    updateRepairFee,
    getRentalFee,   
    updateRentalFee 
};

export default configurationService;