import httpClient from "../http-common";

// Obtener préstamos activos (vigentes y atrasados)
const getActiveLoans = () => {
    return httpClient.get('/REPORT-SERVICE/api/v1/reports/loans/active');
}

// Obtener clientes con atrasos
const getOverdueClients = () => {
    return httpClient.get('/REPORT-SERVICE/api/v1/reports/clients/overdue');
}

// Obtener ranking de herramientas más prestadas
const getTopTools = () => {
    return httpClient.get('/REPORT-SERVICE/api/v1/reports/tools/top');
}

export default { getActiveLoans, getOverdueClients, getTopTools };