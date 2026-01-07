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
    Alert
} from "@mui/material";
import SaveIcon from "@mui/icons-material/Save";
import configurationService from "../services/configuration.service";
import toolService from "../services/tool.service";

const ManageFees = () => {

    const [lateFee, setLateFee] = useState(0);
    const [repairFee, setRepairFee] = useState(0);
    const [tools, setTools] = useState([]);
    const [message, setMessage] = useState("");

    const init = () => {
        configurationService.getLateFee()
            .then(response => setLateFee(response.data))
            .catch(error => console.error("Error loading late fee.", error));

        configurationService.getRepairFee()
            .then(response => setRepairFee(response.data))
            .catch(error => console.error("Error loading repair fee.", error));

        toolService.getAll()
            .then(response => setTools(response.data))
            .catch(error => console.error("Error loading tools.", error));
    };

    useEffect(() => {
        init();
    }, []);

    const showMessage = (text) => {
        setMessage(text);
        setTimeout(() => setMessage(""), 3000);
    };

    const handleSaveLateFee = (e) => {
        e.preventDefault();
        const feeData = { value: parseInt(lateFee, 10) };
        configurationService.updateLateFee(feeData)
            .then(() => showMessage("Tarifa de multa por atraso actualizada exitosamente."))
            .catch(error => console.error("Error updating late fee.", error));
    };

    const handleSaveRepairFee = (e) => {
        e.preventDefault();
        const feeData = { value: parseInt(repairFee, 10) };
        configurationService.updateRepairFee(feeData)
            .then(() => showMessage("Cargo por reparación actualizado exitosamente."))
            .catch(error => console.error("Error updating repair fee.", error));
    };

    const handleReplacementValueChange = (id, value) => {
        setTools(prevTools =>
            prevTools.map(tool =>
                tool.id === id ? { ...tool, replacementValue: value } : tool
            )
        );
    };

    const handleSaveTool = (toolToSave) => {
        toolService.update(toolToSave.id, { ...toolToSave, replacementValue: parseInt(toolToSave.replacementValue, 10) })
            .then(() => showMessage(`Valor de reposición para '${toolToSave.name}' actualizado.`))
            .catch(error => console.error(`Error updating tool ${toolToSave.id}`, error));
    };

    return (
        <Box sx={{ maxWidth: 800, margin: 'auto', mt: 4 }}>
            <Typography variant="h4" gutterBottom>
                Configuración de Tarifas
            </Typography>

            {message && <Alert severity="success" sx={{ mb: 2 }}>{message}</Alert>}

            {/*  */}
            <Paper component="form" onSubmit={handleSaveLateFee} sx={{ p: 2, mb: 4 }}>
                <Typography variant="h6">Tarifa Diaria por Atraso</Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', mt: 2 }}>
                    <TextField
                        label="Monto ($)"
                        type="number"
                        value={lateFee}
                        onChange={(e) => setLateFee(e.target.value)}
                        variant="outlined"
                        size="small"
                        required
                    />
                    <Button type="submit" variant="contained" startIcon={<SaveIcon />} sx={{ ml: 2 }}>
                        Guardar Tarifa
                    </Button>
                </Box>
            </Paper>

            {/*  */}
            <Paper component="form" onSubmit={handleSaveRepairFee} sx={{ p: 2, mb: 4 }}>
                <Typography variant="h6">Cargo Fijo por Daño Reparable</Typography>
                <Box sx={{ display: 'flex', alignItems: 'center', mt: 2 }}>
                    <TextField
                        label="Monto ($)"
                        type="number"
                        value={repairFee}
                        onChange={(e) => setRepairFee(e.target.value)}
                        variant="outlined"
                        size="small"
                        required
                    />
                    <Button type="submit" variant="contained" startIcon={<SaveIcon />} sx={{ ml: 2 }}>
                        Guardar Cargo
                    </Button>
                </Box>
            </Paper>

            {/*  */}
            <Typography variant="h6">Valores de Reposición por Herramienta</Typography>
            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 'bold' }}>Nombre Herramienta</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Categoría</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 'bold' }}>Valor de Reposición ($)</TableCell>
                            <TableCell align="center" sx={{ fontWeight: 'bold' }}>Acción</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {tools.map((tool) => (
                            <TableRow key={tool.id}>
                                <TableCell>{tool.name}</TableCell>
                                <TableCell>{tool.category}</TableCell>
                                <TableCell align="right">
                                    <TextField
                                        type="number"
                                        value={tool.replacementValue}
                                        onChange={(e) => handleReplacementValueChange(tool.id, e.target.value)}
                                        variant="standard"
                                        size="small"
                                        inputProps={{ style: { textAlign: 'right' } }}
                                    />
                                </TableCell>
                                <TableCell align="center">
                                    <Button
                                        variant="outlined"
                                        size="small"
                                        onClick={() => handleSaveTool(tool)}
                                    >
                                        Guardar
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
};

export default ManageFees;