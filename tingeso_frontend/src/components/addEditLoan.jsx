import { useState, useEffect } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import { Box, TextField, Button, FormControl, Typography } from "@mui/material";
import SaveIcon from "@mui/icons-material/Save";
import SearchIcon from '@mui/icons-material/Search';
import { useKeycloak } from "@react-keycloak/web"; //hook de Keycloak
import loanService from "../services/loan.service";
import toolService from "../services/tool.service";
import clientService from "../services/client.service";
import SelectionModal from "./SelectionModal";

const AddEditLoan = () => {
    const { id } = useParams();
    const { keycloak, initialized } = useKeycloak();

    // Estados para los datos del préstamo
    const [toolId, setToolId] = useState("");
    const [dueDate, setDueDate] = useState("");
    const [loanDate, setLoanDate] = useState(new Date().toISOString().split('T')[0]);

    // Estados para la selección de cliente (solo para admin)
    const [clientId, setClientId] = useState("");
    const [clientName, setClientName] = useState("");
    const [clients, setClients] = useState([]);
    const [isClientModalOpen, setClientModalOpen] = useState(false);

    // Estados generales
    const [toolName, setToolName] = useState("");
    const [tools, setTools] = useState([]);
    const [isToolModalOpen, setToolModalOpen] = useState(false);
    const [titleLoanForm, setTitleLoanForm] = useState("Nuevo Préstamo");
    const navigate = useNavigate();
    
    // Determina si el usuario es admin
    const isAdmin = initialized && keycloak.hasRealmRole("ADMIN");

    useEffect(() => {
        toolService.getAll().then(response => setTools(response.data));

        // Carga los clientes SOLO si es admin
        if (isAdmin) {
            clientService.getAll().then(response => setClients(response.data));
        }

        // Lógica para editar un préstamo existente (sin cambios)
        if (id) {
            // ...
        }
    }, [id, isAdmin, initialized]); // Se ejecuta cuando el estado de admin cambie

    const saveLoan = (e) => {
        e.preventDefault();
        
        const loan = { toolId, dueDate };
        
        // 4. Añade el clientId al objeto SOLO si es un admin
        if (isAdmin) {
            loan.clientId = clientId;
        }

        loanService.create(loan)
            .then(() => navigate("/loans"))
            .catch(error => console.error("Error al crear préstamo.", error));
    };

    const handleSelectClient = (client) => {
        setClientId(client.id);
        setClientName(client.name);
    };

    const handleSelectTool = (tool) => {
        setToolId(tool.id);
        setToolName(tool.name);
    };

    if (!initialized) return <div>Cargando...</div>;

    return (
        <Box component="form" onSubmit={saveLoan} sx={{ maxWidth: 500, margin: 'auto', mt: 4 }}>
            <Typography variant="h4" gutterBottom>{titleLoanForm}</Typography>
            <hr />
            
            {/* Muestra la selección de cliente SOLO si es admin */}
            {isAdmin && (
                <FormControl fullWidth sx={{ mt: 2, display: 'flex', flexDirection: 'row', alignItems: 'flex-end' }}>
                    <TextField 
                        label="Cliente Seleccionado" 
                        value={clientName} 
                        required 
                        disabled 
                        fullWidth 
                        variant="outlined" 
                    />
                    <Button variant="contained" onClick={() => setClientModalOpen(true)} startIcon={<SearchIcon />} sx={{ ml: 1 }}>
                        Buscar
                    </Button>
                </FormControl>
            )}

            {/* Selección de Herramienta  */}
            <FormControl fullWidth sx={{ mt: 2, display: 'flex', flexDirection: 'row', alignItems: 'flex-end' }}>
                <TextField label="Herramienta Seleccionada" value={toolName} required disabled fullWidth variant="outlined" />
                <Button variant="contained" onClick={() => setToolModalOpen(true)} startIcon={<SearchIcon />} sx={{ ml: 1 }}>
                    Buscar
                </Button>
            </FormControl>

            {/* Campos de Fecha */}
            <FormControl fullWidth sx={{ mt: 2 }}>
                <TextField label="Fecha de Préstamo" type="date" value={loanDate} disabled InputLabelProps={{ shrink: true }} />
            </FormControl>
            <FormControl fullWidth sx={{ mt: 2 }}>
                <TextField label="Fecha de Devolución Pactada" type="date" value={dueDate} required onChange={(e) => setDueDate(e.target.value)} InputLabelProps={{ shrink: true }} />
            </FormControl>

            <FormControl sx={{ mt: 3 }}>
                <Button variant="contained" color="primary" type="submit" startIcon={<SaveIcon />} disabled={!toolId || !dueDate || (isAdmin && !clientId)}>
                    Guardar
                </Button>
            </FormControl>
            <hr />
            <Link to="/loans">Volver a la Lista</Link>

            {/* Modales */}
            {isAdmin && (
                <SelectionModal
                    open={isClientModalOpen}
                    onClose={() => setClientModalOpen(false)}
                    onSelect={handleSelectClient}
                    data={clients}
                    title="Seleccionar Cliente"
                    columns={[{ field: 'name', headerName: 'Nombre' }, { field: 'rut', headerName: 'RUT' }]}
                />
            )}
            <SelectionModal
                open={isToolModalOpen}
                onClose={() => setToolModalOpen(false)}
                onSelect={handleSelectTool}
                data={tools.filter(t => t.status === 'Disponible')}
                title="Seleccionar Herramienta"
                columns={[{ field: 'name', headerName: 'Nombre' }, { field: 'category', headerName: 'Categoría' }]}
            />
        </Box>
    );
};

export default AddEditLoan;