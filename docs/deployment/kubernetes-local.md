# Deploy to a local Kubernetes cluster (free)

Practise every Kubernetes concept before paying for EKS. Works with Docker Desktop Kubernetes
(Settings → Kubernetes → Enable), kind or minikube.

```bash
kubectl config current-context            # docker-desktop / kind-... / minikube
docker compose --profile app build backend frontend    # builds shopsphere-*:local images
# kind only:     kind load docker-image shopsphere-backend:local shopsphere-frontend:local
# minikube only: minikube image load shopsphere-backend:local && minikube image load shopsphere-frontend:local
bash scripts/deploy.sh local              # creates the Secret from .env, applies overlays/local
kubectl get pods -n shopsphere-local -w
kubectl port-forward svc/frontend 3000:80 -n shopsphere-local
```
Open http://localhost:3000. The frontend's Nginx proxies `/api` to the `backend` Service, so no
Ingress controller is needed locally.

Explore:
```bash
kubectl describe deploy backend -n shopsphere-local    # probes, resources, strategy
kubectl logs deploy/backend -n shopsphere-local
kubectl get hpa,pdb -n shopsphere-local                # HPA shows <unknown> without metrics-server
kubectl kustomize kubernetes/overlays/local | less     # the exact YAML that was applied
```
Clean up: `bash scripts/destroy.sh local`.
