import { useState, useEffect } from "react";
import { 
    Box, Typography, Table, TableBody, TableCell, TableContainer, 
    TableHead, TableRow, Paper, Tabs, Tab, Alert, CircularProgress 
} from "@mui/material";
import AssessmentIcon from '@mui/icons-material/Assessment';
import AssignmentLateIcon from '@mui/icons-material/AssignmentLate';
import TrendingUpIcon from '@mui/icons-material/TrendingUp';
import reportService from "../services/report.service";

const Reports = () => {
    const [tabValue, setTabValue] = useState(0);
    const [loading, setLoading] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");

    // Datos para las tablas
    const [activeLoans, setActiveLoans] = useState([]);
    const [overdueClients, setOverdueClients] = useState([]);
    const [topTools, setTopTools] = useState([]);

    // Cargar datos al cambiar de pestaña
    useEffect(() => {
        loadReportData(tabValue);
    }, [tabValue]);

    const loadReportData = (tabIndex) => {
        setLoading(true);
        setErrorMsg("");

        let request;
        if (tabIndex === 0) request = reportService.getActiveLoans();
        else if (tabIndex === 1) request = reportService.getOverdueClients();
        else if (tabIndex === 2) request = reportService.getTopTools();

        request
            .then(response => {
                if (tabIndex === 0) setActiveLoans(response.data);
                else if (tabIndex === 1) setOverdueClients(response.data);
                else if (tabIndex === 2) setTopTools(response.data);
            })
            .catch(error => {
                console.error("Error fetching report data:", error);
                setErrorMsg("No se pudieron cargar los datos del reporte.");
            })
            .finally(() => setLoading(false));
    };

    const handleTabChange = (event, newValue) => {
        setTabValue(newValue);
    };

    return (
        <Box sx={{ width: '100%', mt: 2 }}>
            <Typography variant="h4" gutterBottom>
                Panel de Reportes y Consultas
            </Typography>
            <hr />

            <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 2 }}>
                <Tabs value={tabValue} onChange={handleTabChange} centered>
                    <Tab icon={<AssignmentLateIcon />} label="Préstamos Activos" />
                    <Tab icon={<AssessmentIcon />} label="Clientes con Atrasos" />
                    <Tab icon={<TrendingUpIcon />} label="Ranking Herramientas" />
                </Tabs>
            </Box>

            {errorMsg && <Alert severity="error" sx={{ mb: 2 }}>{errorMsg}</Alert>}
            
            {loading ? (
                <Box display="flex" justifyContent="center" mt={4}>
                    <CircularProgress />
                </Box>
            ) : (
                <Box>
                    {/* --- TAB 0: Préstamos Activos --- */}
                    {tabValue === 0 && (
                        <TableContainer component={Paper}>
                            <Table>
                                <TableHead sx={{ bgcolor: '#f5f5f5' }}>
                                    <TableRow>
                                        <TableCell><strong>ID Préstamo</strong></TableCell>
                                        <TableCell><strong>Cliente</strong></TableCell>
                                        <TableCell><strong>Herramienta</strong></TableCell>
                                        <TableCell><strong>Fecha Entrega</strong></TableCell>
                                        <TableCell><strong>Fecha Pactada</strong></TableCell>
                                        <TableCell><strong>Estado</strong></TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {activeLoans.length > 0 ? activeLoans.map((loan) => (
                                        <TableRow key={loan.id}>
                                            <TableCell>{loan.id}</TableCell>
                                            <TableCell>{loan.clientName}</TableCell>
                                            <TableCell>{loan.toolName}</TableCell>
                                            <TableCell>{loan.loanDate}</TableCell>
                                            <TableCell>{loan.dueDate}</TableCell>
                                            <TableCell sx={{ 
                                                color: loan.status === 'Atrasado' ? 'red' : 'green', 
                                                fontWeight: 'bold' 
                                            }}>
                                                {loan.status}
                                            </TableCell>
                                        </TableRow>
                                    )) : (
                                        <TableRow><TableCell colSpan={6} align="center">No hay préstamos activos.</TableCell></TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}

                    {/* --- TAB 1: Clientes con Atrasos --- */}
                    {tabValue === 1 && (
                        <TableContainer component={Paper}>
                            <Table>
                                <TableHead sx={{ bgcolor: '#fff3e0' }}>
                                    <TableRow>
                                        <TableCell><strong>ID Cliente</strong></TableCell>
                                        <TableCell><strong>Nombre</strong></TableCell>
                                        <TableCell><strong>RUT</strong></TableCell>
                                        <TableCell><strong>Días de Atraso Acumulados</strong></TableCell>
                                        <TableCell><strong>Multas Pendientes</strong></TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {overdueClients.length > 0 ? overdueClients.map((client) => (
                                        <TableRow key={client.id}>
                                            <TableCell>{client.id}</TableCell>
                                            <TableCell>{client.name}</TableCell>
                                            <TableCell>{client.rut}</TableCell>
                                            <TableCell>{client.totalOverdueDays}</TableCell>
                                            <TableCell>{client.pendingFinesCount}</TableCell>
                                        </TableRow>
                                    )) : (
                                        <TableRow><TableCell colSpan={5} align="center">No hay clientes con atrasos registrados.</TableCell></TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}

                    {/* --- TAB 2: Ranking Herramientas --- */}
                    {tabValue === 2 && (
                        <TableContainer component={Paper}>
                            <Table>
                                <TableHead sx={{ bgcolor: '#e3f2fd' }}>
                                    <TableRow>
                                        <TableCell><strong>Posición</strong></TableCell>
                                        <TableCell><strong>Herramienta</strong></TableCell>
                                        <TableCell><strong>Categoría</strong></TableCell>
                                        <TableCell align="right"><strong>Veces Prestada</strong></TableCell>
                                    </TableRow>
                                </TableHead>
                                <TableBody>
                                    {topTools.length > 0 ? topTools.map((tool, index) => (
                                        <TableRow key={index}>
                                            <TableCell>#{index + 1}</TableCell>
                                            <TableCell>{tool.toolName}</TableCell>
                                            <TableCell>{tool.category}</TableCell>
                                            <TableCell align="right">{tool.loanCount}</TableCell>
                                        </TableRow>
                                    )) : (
                                        <TableRow><TableCell colSpan={4} align="center">No hay datos suficientes para el ranking.</TableCell></TableRow>
                                    )}
                                </TableBody>
                            </Table>
                        </TableContainer>
                    )}
                </Box>
            )}
        </Box>
    );
}

export default Reports;