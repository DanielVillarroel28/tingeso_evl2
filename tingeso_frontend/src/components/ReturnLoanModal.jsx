import {
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    Button,
    FormControl,
    FormControlLabel,
    Radio,
    RadioGroup
} from '@mui/material';
import { useState } from 'react';

const ReturnLoanModal = ({ open, onClose, onSubmit }) => {
    const [toolStatus, setToolStatus] = useState('OK');

    const handleSubmit = () => {
        onSubmit(toolStatus);
        onClose();
    };

    return (
        <Dialog open={open} onClose={onClose}>
            <DialogTitle>Registrar Devolución</DialogTitle>
            <DialogContent>
                <FormControl component="fieldset" sx={{ mt: 2 }}>
                    <p>Por favor, seleccione el estado de la herramienta devuelta:</p>
                    <RadioGroup
                        aria-label="tool-status"
                        value={toolStatus}
                        onChange={(e) => setToolStatus(e.target.value)}
                    >
                        <FormControlLabel value="OK" control={<Radio />} label="En buen estado" />
                        <FormControlLabel value="Dañada" control={<Radio />} label="Dañada (reparable)" />
                        <FormControlLabel value="Irreparable" control={<Radio />} label="Daño irreparable (se cobrará reposición)" />
                    </RadioGroup>
                </FormControl>
            </DialogContent>
            <DialogActions>
                <Button onClick={onClose}>Cancelar</Button>
                <Button onClick={handleSubmit} variant="contained" color="primary">
                    Confirmar Devolución
                </Button>
            </DialogActions>
        </Dialog>
    );
};

export default ReturnLoanModal;