import { useState, useEffect } from "react";
import { Link, useParams, useNavigate, useLocation } from "react-router-dom";
import { Box, TextField, Button, FormControl, MenuItem, Typography } from "@mui/material";
import SaveIcon from "@mui/icons-material/Save";
import clientService from "../services/client.service";

const AddEditClient = () => {
    const { id } = useParams();
    const location = useLocation();
    const navigate = useNavigate();

    const isMyProfile = location.pathname === '/profile/edit';

    // Estados para los campos del formulario (vacíos)
    const [name, setName] = useState("");
    const [rut, setRut] = useState("");
    const [phone, setPhone] = useState("");
    const [email, setEmail] = useState("");
    const [status, setStatus] = useState("Activo");

    const [titleClientForm, setTitleClientForm] = useState("");

    useEffect(() => {
        if (isMyProfile) {
            setTitleClientForm("Editar Mi Perfil");
        } else if (id) {
            setTitleClientForm("Editar Cliente");
        } else {
            setTitleClientForm("Nuevo Cliente");
        }
    }, [id, isMyProfile]);

    const saveClient = (e) => {
        e.preventDefault();
        const client = { name, rut, phone, email, status };

        if (isMyProfile) {
            clientService.updateMyProfile(client)
                .then(() => navigate("/home"))
                .catch(error => console.error("Error al actualizar el perfil.", error));
        } else if (id) {
            clientService.update(id, client)
                .then(() => navigate("/clients"))
                .catch(error => console.error("Error al actualizar el cliente.", error));
        } else {
            clientService.create(client)
                .then(() => navigate("/clients"))
                .catch(error => console.error("Error al crear el cliente.", error));
        }
    };

    return (
        <Box
            component="form"
            onSubmit={saveClient}
            sx={{ maxWidth: 500, margin: 'auto', mt: 4 }}
        >
            <Typography variant="h4" gutterBottom>{titleClientForm}</Typography>
            <hr />
                
            <FormControl fullWidth sx={{ mt: 2 }}>
                <TextField id="name" label="Nombre Completo" value={name} variant="outlined" onChange={(e) => setName(e.target.value)} required />
            </FormControl>

            <FormControl fullWidth sx={{ mt: 2 }}>
                <TextField id="rut" label="RUT" value={rut} variant="outlined" onChange={(e) => setRut(e.target.value)} required />
            </FormControl>

            <FormControl fullWidth sx={{ mt: 2 }}>
                <TextField id="phone" label="Teléfono" value={phone} variant="outlined" onChange={(e) => setPhone(e.target.value)} required />
            </FormControl>

            <FormControl fullWidth sx={{ mt: 2 }}>
                <TextField id="email" label="Email" value={email} variant="outlined" onChange={(e) => setEmail(e.target.value)} required type="email" />
            </FormControl>

            {!isMyProfile && (
                <FormControl fullWidth sx={{ mt: 2 }}>
                    <TextField id="status" label="Estado del Cliente" value={status} select variant="outlined" onChange={(e) => setStatus(e.target.value)}>
                        <MenuItem value={"Activo"}>Activo</MenuItem>
                        <MenuItem value={"Restringido"}>Restringido</MenuItem>
                    </TextField>
                </FormControl>
            )}
            
            <FormControl sx={{ mt: 3 }}>
                <Button variant="contained" color="primary" type="submit" startIcon={<SaveIcon />}>
                    Guardar
                </Button>
            </FormControl>
            <hr />
            <Link to={isMyProfile ? "/home" : "/clients"}>Volver</Link>
        </Box>
    );
};

export default AddEditClient;