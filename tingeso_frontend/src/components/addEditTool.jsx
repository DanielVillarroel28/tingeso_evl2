import { useState, useEffect } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import { Box, TextField, Button, FormControl, Snackbar, Alert } from "@mui/material";
import SaveIcon from "@mui/icons-material/Save";
import toolService from "../services/tool.service";

const AddEditTool = () => {
    const { id } = useParams();
    
    const [name, setName] = useState("");
    const [category, setCategory] = useState("");
    const [replacementValue, setReplacementValue] = useState("");
    const [status, setStatus] = useState("Disponible");
    
    const [quantity, setQuantity] = useState(1); 

    // Estados para control visual y feedback
    const [loading, setLoading] = useState(false);
    const [titleToolForm, setTitleToolForm] = useState("Nueva Herramienta");
    const [openSnackbar, setOpenSnackbar] = useState(false);
    const [errorMsg, setErrorMsg] = useState("");
    
    const navigate = useNavigate();

    const saveTool = async (e) => {
        e.preventDefault();
        setLoading(true);
        setErrorMsg("");

        // Preparamos el objeto base de la herramienta
        const toolData = {
            name,
            category,
            replacementValue: parseInt(replacementValue, 10),
            status,
            availableStock: 1 // Cada herramienta individual tiene stock 1
        };

        try {
            if (id) {
                await toolService.update(id, toolData);
            } else {
                const qty = parseInt(quantity, 10);
                
                if (qty > 1) {
                    // Si el usuario pidió más de 1, bucle
                    const promises = [];
                    for (let i = 0; i < qty; i++) {
                        promises.push(toolService.create(toolData));
                    }
                    // Esperamos a que TODAS se creen antes de continuar
                    await Promise.all(promises);
                } else {
                    // Si es 1, creamos normal
                    await toolService.create(toolData);
                }
            }
            
            // Si todo salió bien, volvemos a la lista
            navigate("/tools");
        } catch (error) {
            console.error("Error al guardar:", error);
            setErrorMsg("Hubo un error al guardar las herramientas. Revisa la consola.");
            setOpenSnackbar(true);
        } finally {
            setLoading(false);
        }
    };

    //CARGAR DATOS SI ES EDICIÓN
    useEffect(() => {
        if (id) {
            setTitleToolForm("Editar Herramienta");
            toolService.get(id)
                .then((response) => {
                    const tool = response.data;
                    setName(tool.name);
                    setCategory(tool.category);
                    setReplacementValue(tool.replacementValue);
                    setStatus(tool.status);
                })
                .catch((error) => console.error("Error obteniendo herramienta", error));
        }
    }, [id]);

    return (
        <Box component="form" onSubmit={saveTool} sx={{ maxWidth: 500, margin: 'auto', mt: 4, p: 2, boxShadow: 3, borderRadius: 2 }}>
            <h3>{titleToolForm}</h3>
            <hr />

            {/* Campos Estándar */}
            <FormControl fullWidth sx={{ mt: 2 }}>
                <TextField
                    id="name"
                    label="Nombre de la Herramienta"
                    value={name}
                    variant="outlined"
                    onChange={(e) => setName(e.target.value)}
                    required
                />
            </FormControl>

            <FormControl fullWidth sx={{ mt: 2 }}>
                <TextField
                    id="category"
                    label="Categoría"
                    value={category}
                    variant="outlined"
                    onChange={(e) => setCategory(e.target.value)}
                    required
                />
            </FormControl>

            <FormControl fullWidth sx={{ mt: 2 }}>
                <TextField
                    id="replacementValue"
                    label="Valor de Reposición ($)"
                    type="number"
                    value={replacementValue}
                    variant="outlined"
                    onChange={(e) => setReplacementValue(e.target.value)}
                    required
                />
            </FormControl>

            {!id && (
                <FormControl fullWidth sx={{ mt: 3, bgcolor: '#f5f5f5', p: 1, borderRadius: 1 }}>
                    <TextField
                        id="quantity"
                        label="¿Cuántas copias quieres crear? (Max 50)"
                        type="number"
                        value={quantity}
                        variant="outlined"
                        inputProps={{ min: 1, max: 50 }} // Límite de seguridad
                        onChange={(e) => setQuantity(e.target.value)}
                        required
                        helperText={`Se crearán ${quantity} herramientas idénticas en el sistema.`}
                    />
                </FormControl>
            )}

            <FormControl sx={{ mt: 3, width: '100%' }}>
                <Button 
                    variant="contained" 
                    color="primary" 
                    type="submit" 
                    startIcon={<SaveIcon />}
                    disabled={loading}
                    size="large"
                >
                    {loading ? (id ? "Actualizando..." : "Creando herramientas...") : "Guardar"}
                </Button>
            </FormControl>

            <Box sx={{ mt: 2, textAlign: 'center' }}>
                <Link to="/tools/list" style={{ textDecoration: 'none', color: '#1976d2' }}>
                    Volver a la Lista
                </Link>
            </Box>

            {/* Mensaje de error/feedback */}
            <Snackbar
                open={openSnackbar}
                autoHideDuration={6000}
                onClose={() => setOpenSnackbar(false)}
            >
                <Alert onClose={() => setOpenSnackbar(false)} severity="error" sx={{ width: '100%' }}>
                    {errorMsg}
                </Alert>
            </Snackbar>
        </Box>
    );
};

export default AddEditTool;