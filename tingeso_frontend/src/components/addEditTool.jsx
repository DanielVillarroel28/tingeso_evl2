import { useState, useEffect } from "react";
import { Link, useParams, useNavigate } from "react-router-dom";
import { Box, TextField, Button, FormControl } from "@mui/material";
import SaveIcon from "@mui/icons-material/Save";
import toolService from "../services/tool.service";

const AddEditTool = () => {
    const { id } = useParams();
    const [name, setName] = useState("");
    const [category, setCategory] = useState("");
    const [replacementValue, setReplacementValue] = useState("");
    const [status, setStatus] = useState("Disponible"); // Estado por defecto
    const [availableStock, setAvailableStock] = useState("");

    const [titleToolForm, setTitleToolForm] = useState("Nueva Herramienta");
    const navigate = useNavigate();

    const saveTool = (e) => {
        e.preventDefault();

        const tool = {
            id,
            name,
            category,
            replacementValue: parseInt(replacementValue, 10),
            status,
            availableStock: parseInt(availableStock, 10) || 0
        };

        if (id) {
            // Lógica para actualizar
            toolService.update(id, tool)
                .then(() => navigate("/tools/list"))
                .catch(error => console.error("Error al actualizar la herramienta.", error));
        } else {
            // Crea una nueva herramienta individual
            toolService.create(tool)
                .then(() => navigate("/tools/list"))
                .catch(error => console.error("Error al crear la herramienta.", error));
        }
    };

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
                .catch((error) => console.error("Error al obtener la herramienta.", error));
        }
    }, [id]);

    return (
        <Box component="form" onSubmit={saveTool} sx={{ maxWidth: 500, margin: 'auto', mt: 4 }}>
            <h3>{titleToolForm}</h3>
            <hr />
                
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
                    label="Valor de Reposición"
                    type="number"
                    value={replacementValue}
                    variant="outlined"
                    onChange={(e) => setReplacementValue(e.target.value)}
                    required
                />
            </FormControl>

            <FormControl sx={{ mt: 3 }}>
                <Button variant="contained" color="primary" type="submit" startIcon={<SaveIcon />}>
                    Guardar
                </Button>
            </FormControl>
            <hr />
            <Link to="/tools/list">Volver a la Lista</Link>
        </Box>
    );
};

export default AddEditTool;