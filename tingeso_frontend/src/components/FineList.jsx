import { useEffect, useState } from "react";
import fineService from "../services/fine.service";
import {
    Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Button, Chip, Typography, Box
} from "@mui/material";
import PaidIcon from '@mui/icons-material/Paid';
import { useKeycloak } from "@react-keycloak/web";

const FineList = () => {
    const [fines, setFines] = useState([]);
    const { keycloak, initialized } = useKeycloak();

    useEffect(() => {
       // Autenticación del usuario
        if (!initialized || !keycloak.authenticated) {
            return;
        }

        console.log("Keycloak inicializado. Verificando roles...");
        console.log("Roles del usuario:", keycloak.realmAccess?.roles);

        const isAdmin = keycloak.hasRealmRole("ADMIN");
        console.log("¿El usuario es ADMIN?", isAdmin);

        if (isAdmin) {
            console.log("Decisión: El usuario es ADMIN. Solicitando todas las multas.");
            fineService.getAll()
                .then(response => {
                    setFines(response.data);
                })
                .catch(error => {
                    console.error("Error al solicitar todas las multas (ADMIN):", error);
                    if (error.response?.status === 403) {
                        alert("Acceso denegado. Tu token no parece tener el rol de ADMIN a pesar de la verificación.");
                    }
                });
        } else {
            console.log("Decisión: El usuario es USER. Solicitando solo sus multas.");
            fineService.getMyFines()
                .then(response => {
                    setFines(response.data);
                })
                .catch(error => console.error("Error al solicitar las multas del usuario (USER):", error));
        }

    }, [initialized, keycloak.authenticated, keycloak.token]); // Se vuelve a ejecutar si el token cambia

    const reloadFines = () => {
        // Esta función recarga la lista correcta según el rol
        if (!initialized || !keycloak.authenticated) return;

        if (keycloak.hasRealmRole("ADMIN")) {
            fineService.getAll().then(response => setFines(response.data));
        } else {
            fineService.getMyFines().then(response => setFines(response.data));
        }
    };

    const handlePayFine = (id) => {
        if (window.confirm("¿Confirmar el pago de esta multa?")) {
            fineService.pay(id)
                .then(() => {
                    alert("Multa pagada exitosamente.");
                    reloadFines(); // Llama a la función de recarga
                })
                .catch(error => alert("Error: " + (error.response?.data?.message || "Ocurrió un error.")));
        }
    };

    if (!initialized) {
        return <Typography>Cargando autenticación...</Typography>;
    }

    if (!keycloak.authenticated) {
        return <Typography>Por favor, inicie sesión para ver las multas.</Typography>;
    }

    return (
        <Box sx={{ margin: 2 }}>
            <Typography variant="h4" gutterBottom>
                Gestión de Multas
            </Typography>
            <TableContainer component={Paper}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell sx={{ fontWeight: 'bold' }}>ID Multa</TableCell>
                            {/* Mostrar la columna Cliente solo si es admin */}
                            {keycloak.hasRealmRole("ADMIN") && <TableCell sx={{ fontWeight: 'bold' }}>Cliente</TableCell>}
                            <TableCell sx={{ fontWeight: 'bold' }}>Herramienta</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Tipo</TableCell>
                            <TableCell align="right" sx={{ fontWeight: 'bold' }}>Monto</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Estado</TableCell>
                            <TableCell sx={{ fontWeight: 'bold' }}>Acciones</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {fines.map((fine) => (
                            <TableRow key={fine.id}>
                                <TableCell>{fine.id}</TableCell>
                                {keycloak.hasRealmRole("ADMIN") && <TableCell>{fine.clientName}</TableCell>}
                                <TableCell>{fine.toolName}</TableCell>
                                <TableCell>{fine.fineType}</TableCell>
                                <TableCell align="right">${fine.amount.toLocaleString('es-CL')}</TableCell>
                                <TableCell>
                                    <Chip label={fine.status} color={fine.status === 'Pendiente' ? 'error' : 'success'} size="small" />
                                </TableCell>
                                <TableCell>
                                    {fine.status === 'Pendiente' && (
                                        <Button variant="contained" color="success" size="small" onClick={() => handlePayFine(fine.id)} startIcon={<PaidIcon />}>
                                            Pagar
                                        </Button>
                                    )}
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
};

export default FineList;