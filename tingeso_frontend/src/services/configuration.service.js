import http from "../http-common";

// --- TARIFA DE MULTA POR ATRASO ---
const getLateFee = () => {
    return http.get("/FINE-SERVICE/config/late-fee");
};

const updateLateFee = (data) => {
    return http.put("/FINE-SERVICE/config/late-fee", data);
};

// --- CARGO POR REPARACIÓN ---
const getRepairFee = () => {
    return http.get("/FINE-SERVICE/config/repair-fee");
};

const updateRepairFee = (data) => {
    return http.put("/FINE-SERVICE/config/repair-fee", data);
};

// --- TARIFA DIARIA DE ARRIENDO (FALTABA ESTO) ---
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
    getRentalFee,   // <--- Agregado
    updateRentalFee // <--- Agregado
};

export default configurationService;