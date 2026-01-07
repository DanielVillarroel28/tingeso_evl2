import { useEffect, useState } from "react";
import {
    Box,
    TextField,
    Button,
    Paper,
    Typography,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    MenuItem
} from "@mui/material";
import kardexService from "../services/kardex.service";
import toolService from "../services/tool.service";

const KardexView = () => {
    const [movements, setMovements] = useState([]);
    const [uniqueToolNames, setUniqueToolNames] = useState([]); 
    const [selectedToolName, setSelectedToolName] = useState(''); 
    const [startDate, setStartDate] = useState('');
    const [endDate, setEndDate] = useState('');

    const init = () => {
        // Cargar movimientos con validación de seguridad
        kardexService.getMovements()
            .then(response => {
                if (Array.isArray(response.data)) {
                    setMovements(response.data);
                } else {
                    console.error("La API de Kardex no devolvió una lista:", response.data);
                    setMovements([]);
                }
            })
            .catch(error => {
                console.error("Error al cargar el kardex:", error);
                setMovements([]);
            });

        // Cargar herramientas para el filtro
        toolService.getAll()
            .then(response => {
                if (Array.isArray(response.data)) {
                    const names = [...new Set(response.data.map(tool => tool.name.toLowerCase()))];
                    setUniqueToolNames(names.sort());
                }
            })
            .catch(error => console.error("Error al cargar herramientas para filtros:", error));
    };

    useEffect(() => { init(); }, []);

    const handleFilter = () => {
        const params = {};
        if (selectedToolName) params.toolName = selectedToolName;
        if (startDate) params.startDate = startDate;
        if (endDate) params.endDate = endDate;

        kardexService.getMovements(params)
            .then(response => {
                if (Array.isArray(response.data)) {
                    setMovements(response.data);
                } else {
                    setMovements([]);
                }
            })
            .catch(error => {
                console.error("Error al filtrar movimientos:", error);
                setMovements([]);
            });
    };

    const handleClearFilters = () => {
        setSelectedToolName('');
        setStartDate('');
        setEndDate('');
        
        // Recargar todo
        kardexService.getMovements()
            .then(response => {
                if (Array.isArray(response.data)) {
                    setMovements(response.data);
                } else {
                    setMovements([]);
                }
            })
            .catch(() => setMovements([]));
    };
    
    const formatDateTime = (dateTimeString) => {
        if (!dateTimeString) return 'N/A';
        return new Date(dateTimeString).toLocaleString('es-CL');
    };

    return (
        <Box sx={{ margin: 2 }}>
            <Typography variant="h4" gutterBottom>
                Historial de Movimientos (Kardex)
            </Typography>

            <Paper sx={{ p: 2, mb: 2, display: 'flex', gap: 2, alignItems: 'center', flexWrap: 'wrap' }}>
                <TextField
                    select
                    label="Herramienta"
                    value={selectedToolName}
                    onChange={(e) => setSelectedToolName(e.target.value)}
                    variant="outlined"
                    size="small"
                    sx={{ minWidth: 200 }}
                >
                    <MenuItem value=""><em>Todas</em></MenuItem>
                    {uniqueToolNames.map(name => (
                        <MenuItem key={name} value={name}>
                            {name.charAt(0).toUpperCase() + name.slice(1)}
                        </MenuItem>
                    ))}
                </TextField>
                <TextField label="Fecha Desde" type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} InputLabelProps={{ shrink: true }} size="small" />
                <TextField label="Fecha Hasta" type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} InputLabelProps={{ shrink: true }} size="small" />
                <Button variant="contained" onClick={handleFilter}>Filtrar</Button>
                <Button variant="outlined" onClick={handleClearFilters}>Limpiar</Button>
            </Paper>

            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 'bold' }}>Fecha</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Herramienta</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Tipo de Movimiento</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 'bold' }}>Cantidad Afectada</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Responsable</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {movements.length > 0 ? (
                            movements.map((movement) => (
                                <TableRow key={movement.id}>
                                    <TableCell>{formatDateTime(movement.movementDate)}</TableCell>
                                    {/* CORRECCIÓN: Usamos toolName y toolId planos, según tu KardexEntity */}
                                    <TableCell>
                                        {movement.toolName || 'Desconocida'} 
                                        {movement.toolId ? ` (ID: ${movement.toolId})` : ''}
                                    </TableCell>
                                    <TableCell>{movement.movementType}</TableCell>
                                    <TableCell align="right">{movement.quantityAffected}</TableCell>
                                    <TableCell>{movement.userResponsible}</TableCell>
                                </TableRow>
                            ))
                        ) : (
                            <TableRow>
                                <TableCell colSpan={5} align="center">
                                    No hay movimientos registrados.
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
};

export default KardexView;