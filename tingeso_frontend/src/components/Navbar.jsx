import * as React from "react";
import AppBar from "@mui/material/AppBar";
import Box from "@mui/material/Box";
import Toolbar from "@mui/material/Toolbar";
import Typography from "@mui/material/Typography";
import Button from "@mui/material/Button";
import IconButton from "@mui/material/IconButton";
import MenuIcon from "@mui/icons-material/Menu";
import Sidemenu from "./Sidemenu";
import { useState } from "react";
import { useKeycloak } from "@react-keycloak/web";
import { Link, useNavigate } from "react-router-dom";

export default function Navbar() {
  const [open, setOpen] = useState(false);
  const { keycloak, initialized } = useKeycloak();
  const navigate = useNavigate();

  const toggleDrawer = (openState) => (event) => {
    setOpen(openState);
  };

  const handleProfileClick = () => {
    navigate("/profile/edit");
  };

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="fixed">
        <Toolbar>
          <IconButton
            size="large"
            edge="start"
            color="inherit"
            aria-label="menu"
            sx={{ mr: 2 }}
            onClick={toggleDrawer(true)}
          >
            <MenuIcon />
          </IconButton>

          <Link 
            to="/home" 
            style={{ textDecoration: 'none', color: 'inherit', flexGrow: 1 }}
          >
            <Typography variant="h5" component="div">
              ToolRent: Tu herramienta definitiva
            </Typography>
          </Link>


          {initialized && (
            <>
              {keycloak.authenticated ? (
                <>
                  <Button
                    color="inherit"
                    onClick={handleProfileClick}
                    sx={{ textTransform: "none", mr: 1 }}
                  >
                    <Typography>
                      {keycloak.tokenParsed?.preferred_username ||
                        keycloak.tokenParsed?.email}
                    </Typography>
                  </Button>
                  <Button color="inherit" onClick={() => keycloak.logout()}>
                    Logout
                  </Button>
                </>
              ) : (
                <Button color="inherit" onClick={() => keycloak.login()}>
                  Login
                </Button>
              )}
            </>
          )}
        </Toolbar>
      </AppBar>

      <Toolbar />
      <Sidemenu open={open} toggleDrawer={toggleDrawer} />
    </Box>
  );
}